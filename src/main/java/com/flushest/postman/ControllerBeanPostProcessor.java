package com.flushest.postman;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.flushest.postman.model.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
public class ControllerBeanPostProcessor implements BeanPostProcessor {

    private static Set<Class> primitiveClasses = new HashSet<>();
    private static Map<Class, Object> defaultValues = new HashMap<>();

    static {
        primitiveClasses.add(Integer.class);
        primitiveClasses.add(Long.class);
        primitiveClasses.add(Boolean.class);
        primitiveClasses.add(Float.class);
        primitiveClasses.add(Double.class);
        primitiveClasses.add(String.class);
        primitiveClasses.add(Character.class);
        primitiveClasses.add(Date.class);
        primitiveClasses.add(Short.class);
        primitiveClasses.add(BigInteger.class);
        primitiveClasses.add(BigDecimal.class);

        defaultValues.put(Integer.class, 0);
        defaultValues.put(Long.class, 0L);
        defaultValues.put(Float.class, 0.1F);
        defaultValues.put(Double.class, 0.1D);
        defaultValues.put(Short.class, 0);
        defaultValues.put(BigInteger.class, 0);
        defaultValues.put(BigDecimal.class, 0.1D);
        defaultValues.put(Character.class, ' ');
        defaultValues.put(String.class, " ");
        defaultValues.put(Boolean.class, false);
        defaultValues.put(Date.class, "yyyy-MM-dd HH:mm");


    }

    @Autowired
    private Config config;


    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(AnnotationUtils.getAnnotation(bean.getClass(), Controller.class) != null) { //Controller对象
            PostManCollection collection = new PostManCollection()
                    .setInfo(new PostManInfo()
                            .set_postman_id(UUID.randomUUID().toString())
                            .setName(beanName)
                            .setDescription(bean.getClass().toGenericString()));

            List<PostManItem> items = new ArrayList<>();
            collection.setItem(items);

            //获取类上路径
            List<String> controllerPath = handleRequestPath(AnnotatedElementUtils.findMergedAnnotation(bean.getClass(), RequestMapping.class));
            //遍历所有方法
            for(Method method : bean.getClass().getMethods()) {
                PostManItem item = new PostManItem()
                        .setName(method.getName());
                PostManRequest request = new PostManRequest();
                item.setRequest(request);

                //取得路径 AnnotationUtils.getAnnotation方法无法传递属性值
                RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if(methodMapping == null) continue;
                List<String> methodPath = handleRequestPath(methodMapping);
                methodPath.addAll(0, controllerPath);

                //跳过无path映射方法
                if(methodPath.isEmpty()) continue;

                //确定请求方式
                RequestMethod[] requestMethods = methodMapping.method();
                RequestMethod requestMethod = requestMethods.length==0? RequestMethod.GET:requestMethods[0];
                request.setMethod(requestMethod);

                //请求url
                PostManRequestUrl url = new PostManRequestUrl()
                        .setHost(config.getHost())
                        .setPort(config.getPort())
                        .setPath(methodPath);
                request.setUrl(url);

                switch (requestMethod) {
                    case GET:
                        request.setBody(Collections.EMPTY_MAP);
                        request.setHeader(Collections.EMPTY_LIST);
                        url.setQuery(getQuery(method));
                        break;
                    case POST:
                        request.setHeader(Arrays.asList(
                                new PostManRequestHeader()
                                        .setKey("Content-Type")
                                        .setName("Content-Type")
                                        .setType("text")
                                        .setValue("application/json")
                        )).setBody(generatePostRequestBody(method));
                        break;
                    default:
                            throw new UnsupportedOperationException("不支持的请求方式:" + requestMethod.name());

                }

                items.add(item);
            }
            writeToJSON(collection);
        }
        return bean;
    }


    /**
     * 获取请求映射路径
     * @param requestMapping
     * @return
     */
    private List<String> handleRequestPath(RequestMapping requestMapping) {
        String[] paths = (String[]) AnnotationUtils.getValue(requestMapping);
        return paths==null || paths.length==0 ? new ArrayList<>() : splitPath(paths[0]);
    }

    /**
     * 划分path为list
     * @param path
     * @return
     */
    private List<String> splitPath(String path) {
        String[] paths = path.split("/");
        List<String> pathList = new ArrayList<>();

        for(String pathString : paths) {
            if(!pathString.trim().isEmpty()) {
                pathList.add(pathString);
            }
        }
        return pathList;
    }


    /**
     * 根据方法参数获取
     * @param method
     * @return
     */
    private List<PostManRequestQuery> getQuery(Method method) {
        ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        Class[] classes = method.getParameterTypes();

        List<PostManRequestQuery>  queries = new ArrayList<>();

        for(int i=0; i<parameterNames.length; i++) {
            Class clazz = classes[i];
            if(isPrimitive(clazz)) { //原始数据类型
                PostManRequestQuery query = new PostManRequestQuery()
                        .setKey(parameterNames[i])
                        .setValue(null)
                        .setDescription(clazz.getName());
                queries.add(query);
            }else {
                queries.addAll(combineClassFields(clazz));
            }
        }

        return queries;
    }

    /**
     * 判断类型是否是原始数据类型
     * @param clazz
     * @return
     */
    private boolean isPrimitive(Class clazz) {
        return clazz!=null && (clazz.isPrimitive()||primitiveClasses.contains(clazz));
    }


    /**
     * 处理复合类字段
     * @param clazz
     * @return
     */
    private List<PostManRequestQuery> combineClassFields(Class clazz) {
        List<PostManRequestQuery> queries = new ArrayList<>();
        for(Method method : clazz.getMethods()) {
            if(method.getName().startsWith("set")) {
                String fieldName = lowerCaseInitial(method.getName().substring(3).trim());
                if(fieldName.isEmpty()) continue;
                //获取字段类型
                Class type = method.getParameterTypes()[0];
                if(type == clazz) continue;
                if (isPrimitive(type)) {//原始数据类型
                    PostManRequestQuery query = new PostManRequestQuery()
                            .setKey(fieldName)
                            .setValue(null)
                            .setDescription(type.getName());
                    queries.add(query);
                }else if(type.getCanonicalName().startsWith(config.getBasePackage())){
                    queries.addAll(combineClassFields(type));
                }
            }
        }
        return queries;
    }

    /**
     * JSON解析字符串
     * @param obj
     * @return
     */
    private String toJSONString(Object obj) {
        return JSONObject.toJSONString(obj, SerializerFeature.WriteMapNullValue, SerializerFeature.SortField,SerializerFeature.PrettyFormat,SerializerFeature.WriteEnumUsingToString);
    }

    /**
     * JSON解析字符串
     * @param method
     * @return
     */
    private String toJSONString(Method method) {
        ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        Parameter[] parameters = method.getParameters();

        JSONObject jsonObject = new JSONObject(true);
        for(int i=0; i<parameterNames.length; i++) {
            Class clazz = parameters[i].getType();
            String name = parameterNames[i];

            if(isPrimitive(clazz)) {
                jsonObject.put(name, "");
            }else if(clazz.getCanonicalName().startsWith(config.getBasePackage())){
                jsonObject.putAll(parseClassToJSON(clazz));
            }else if(Collection.class.isAssignableFrom(clazz)) { //泛型
                Class subType = (Class) ((ParameterizedType)parameters[i].getParameterizedType()).getActualTypeArguments()[0];
                JSONArray jsonArray = new JSONArray();

                if(isPrimitive(subType)) {
                    jsonArray.add(defaultValues.getOrDefault(subType, " "));
                }else if(subType.getCanonicalName().startsWith(config.getBasePackage())){
                    jsonArray.add(parseClassToJSON(clazz));
                }

                jsonObject.put(name, jsonArray);
            }
        }

        return toJSONString(jsonObject);
    }

    /**
     * 根据类对象解析结构返回类JSON数据
     * @param clazz
     * @return
     */
    private Map<String, Object> parseClassToJSON(Class clazz) {
        Map<String, Object> map = new LinkedHashMap<>();
        for(Method method : clazz.getMethods()) {
            if(method.getName().startsWith("set")) {
                String fieldName = lowerCaseInitial(method.getName().substring(3).trim());
                if(fieldName.isEmpty()) continue;
                //获取字段类型
                Class type = method.getParameterTypes()[0];
                if(type == clazz) continue;
                if (isPrimitive(type)) {//原始数据类型
                    map.put(fieldName, defaultValues.getOrDefault(type, " "));
                }else if(type.getCanonicalName().startsWith(config.getBasePackage())){//业务对象
                    map.put(fieldName, parseClassToJSON(type));
                }else if(Map.class.isAssignableFrom(type)) {//字典对象
                    map.put(fieldName, Collections.emptyMap());
                }else if(Collection.class.isAssignableFrom(type)) {//列表对象
                    ResolvableType resolvableType = ResolvableType.forClass(type);
                    ResolvableType genericType = resolvableType.getGeneric(0);
                    if (isPrimitive(type)) {//原始数据类型
                        map.put(fieldName, new Object[]{defaultValues.getOrDefault(type, " ")});
                    }else if(type.getCanonicalName().startsWith(config.getBasePackage())){//业务对象
                        map.put(fieldName, new Map[]{parseClassToJSON(type)});
                    }
                }
            }
        }
        return map;
    }

    /**
     * 写入至json文件
     * @param collection
     */
    private void writeToJSON(PostManCollection collection) {
        File file = createFile(config.getDir() + File.separator + collection.getInfo().getName() + ".postman_collection.json");

        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(toJSONString(collection).getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("持久化json文件异常");
        }
    }

    /**
     * 创建文件
     * @param name
     */
    private File createFile(String name) {
        File file = new File(name);
        if(!file.exists()) {
            try {
                if(!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(String.format("创建文件%s失败", name), e);
            }
        }
        return file;
    }

    /**
     * 首字母转小写
     * @param src
     * @return
     */
    private String lowerCaseInitial(String src) {
        if(src.isEmpty()) return src;

        char ch = src.charAt(0);
        if(ch >= 'A' && ch <= 'Z') {
            ch += 32;
        }else {
            return src;
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ch);
        stringBuffer.append(src.substring(1));

        return stringBuffer.toString();
    }


    private JSONArray toJSONArray(Method method) {
        ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        Class[] classes = method.getParameterTypes();

        JSONArray jsonArray = new JSONArray();

        for(int i=0; i<parameterNames.length; i++) {
            Class clazz = classes[i];

            if(clazz == MultipartFile.class) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("key", parameterNames[i]);
                jsonObject.put("value",null);
                jsonObject.put("type", "file");
                jsonArray.add(jsonObject);
            }else {
                if(isPrimitive(clazz)) { //原始数据类型
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("key", parameterNames[i]);
                    jsonObject.put("value",null);
                    jsonObject.put("type", "text");
                    jsonArray.add(jsonObject);
                }else {

                }
            }
        }

        return jsonArray;
    }


    /**
     * POST方式生成body
     * @param method
     * @return
     */
    private Map<String, Object> generatePostRequestBody(Method method) {
        Map<String, Object> body = new HashMap<>();
        String mode = "raw";
        for(Class parameterClass : method.getParameterTypes()) {
            if(parameterClass == MultipartFile.class) {
                mode = "formdata";
            }
        }

        body.put("mode", mode);

        switch (mode) {
            case "raw":
                body.put(mode, toJSONString(method));
                break;
            case "formdata":
                body.put(mode, toJSONArray(method));
        }
        return body;
    }
}
