package com.flushest.postman;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.flushest.postman.model.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
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
    }

    @Autowired
    private Config config;


    @ConditionalOnMissingBean(Config.class)
    @Bean
    public Config defaultConfig() {
        return new Config()
                .setHost(Arrays.asList("localhost"))
                .setPort("10010")
                .setDir("D://postman")
                .setBasePackage("");
    }


    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean.getClass().isAnnotationPresent(RestController.class)) { //Controller对象
            PostManCollection collection = new PostManCollection()
                    .setInfo(new PostManInfo()
                            .set_postman_id(UUID.randomUUID().toString())
                            .setName(beanName)
                            .setDescription(bean.getClass().toGenericString()));

            List<PostManItem> items = new ArrayList<>();
            collection.setItem(items);

            //获取类上路径
            List<String> controllerPath = handleRequestPath(bean.getClass().getAnnotation(RequestMapping.class));
            //遍历所有方法
            for(Method method : bean.getClass().getMethods()) {
                PostManItem item = new PostManItem()
                        .setName(method.getName());
                PostManRequest request = new PostManRequest();
                item.setRequest(request);

                //取得路径
                RequestMap methodMapping = getRequestMap(method);
                if(methodMapping == null) continue;
                List<String> methodPath = handleRequestPath(methodMapping);
                methodPath.addAll(0, controllerPath);

                //确定请求方式
                request.setMethod(methodMapping.getMethod());

                //请求url
                PostManRequestUrl url = new PostManRequestUrl()
                        .setHost(config.getHost())
                        .setPort(config.getPort())
                        .setPath(methodPath);
                request.setUrl(url);

                switch (methodMapping.getMethod()) {
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
                            throw new UnsupportedOperationException("不支持的请求方式:" + methodMapping.getMethod().name());

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
    private List<String> handleRequestPath(RequestMap requestMapping) {
        if(requestMapping == null || requestMapping.getPath().length == 0) return Collections.EMPTY_LIST;
        return splitPath(requestMapping.getPath()[0]);
    }


    /**
     * 获取请求映射路径
     * @param requestMapping
     * @return
     */
    private List<String> handleRequestPath(RequestMapping requestMapping) {
        if(requestMapping == null || requestMapping.value().length == 0) return Collections.EMPTY_LIST;
        return splitPath(requestMapping.value()[0]);
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
        return clazz.isPrimitive()||primitiveClasses.contains(clazz);
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

        JSONObject jsonObject = new JSONObject();
        for(int i=0; i<parameterNames.length; i++) {
            Class clazz = parameters[i].getType();
            String name = parameterNames[i];

            if(isPrimitive(clazz)) {
                jsonObject.put(name, "");
            }else if(clazz.getCanonicalName().startsWith(config.getBasePackage())){
                jsonObject.putAll(JSONObject.parseObject(toJSONString(newInstance(clazz))));
            }else if(Collection.class.isAssignableFrom(clazz)) { //泛型
                Class subType = (Class) ((ParameterizedType)parameters[i].getParameterizedType()).getActualTypeArguments()[0];
                JSONArray jsonArray = new JSONArray();

                if(isPrimitive(subType)) {
                    jsonArray.add("");
                }else if(subType.getCanonicalName().startsWith(config.getBasePackage())){
                    jsonArray.add(JSONObject.parseObject(toJSONString(newInstance(subType))));
                }

                jsonObject.put(name, jsonArray);
            }
        }

        return toJSONString(jsonObject);
    }

    /**
     * 反射创建一个类对象
     * @param clazz
     * @return
     */
    private Object newInstance(Class clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("反射创建新对象失败：" + clazz.toGenericString());
        }
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
     * 取方法上Mapping参数
     * @param method
     * @return
     */
    private RequestMap getRequestMap(Method method) {
        RequestMap map = new RequestMap();
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if(requestMapping != null) {
            map.setPath(requestMapping.value());
            RequestMethod requestMethod = requestMapping.method().length > 0? requestMapping.method()[0]: RequestMethod.GET;
            map.setMethod(requestMethod);
        }else {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if(getMapping != null) {
                map.setPath(getMapping.value());
                map.setMethod(RequestMethod.GET);
            }else {
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                if(postMapping != null) {
                    map.setPath(postMapping.value());
                    map.setMethod(RequestMethod.POST);
                }else {
                    return null;
                }
            }
        }
        return map;
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
