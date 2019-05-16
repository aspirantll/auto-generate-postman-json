package com.flushest.postman;

import java.util.List;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
public class Config {

    private List<String> host;

    private String port;

    private String dir;

    private String basePackage;

    public List<String> getHost() {
        return host;
    }

    public Config setHost(List<String> host) {
        this.host = host;
        return this;
    }

    public String getPort() {
        return port;
    }

    public Config setPort(String port) {
        this.port = port;
        return this;
    }

    public String getDir() {
        return dir;
    }

    public Config setDir(String dir) {
        this.dir = dir;
        return this;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public Config setBasePackage(String basePackage) {
        this.basePackage = basePackage;
        return this;
    }
}
