# 自动生成PostMan-JSON工具
## 1.简介
PostMan用于测试web后端接口，但每每添加接口时，需要手动填充很多字段。本工具可以根据SpringMVC中@RestController、@RequestMapping、@GetMapping、@PostMapping注解，自动生成相应的postman配置json，以供导入PostMan使用。
## 2.使用方法
### 2.1  打包jar
先下载源码，然后执行mvn clean install命令将其打包成jar导入到maven仓库。
### 2.2  引入Maven依赖
```
    <dependency>
            <groupId>com.flushest</groupId>
            <artifactId>auto-generate-postman-json</artifactId>
            <version>1.0-SNAPSHOT</version>
    </dependency>
```
### 2.3  配置Config
com.flushest.postman.Config类是核心配置类，需要在代码中定义Bean，以便于加载。
```java
     @Bean
     public Config config() {
            return new Config()
                    .setHost(Arrays.asList("localhost")) //主机地址，如果是192.168.10.0这种形式，应该用字符串数组[192,168,10,0]
                    .setPort("10010") //应用端口
                    .setDir("D://postman")//json生成目录
                    .setBasePackage("");//常用业务Bean所在基础包名
     }
```

### 2.4  启用工具
在启动类上添加@EnableAutoGenerateJson注解

## 3.业务代码规范
### 2.1 控制器
控制器使用@RestController定义
### 2.2 请求路径
请求路径使用@RequestMapping、@GetMapping、PostMapping三种形式，并且应该以路径作为value
形如:@RequestMapping("\path")
### 3.请求方式
暂时支持GET和POST请求
