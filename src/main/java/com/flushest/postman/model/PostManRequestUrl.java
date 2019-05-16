package com.flushest.postman.model;

import java.util.List;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
public class PostManRequestUrl {
    private List<String> host;
    private String port;
    private List<String> path;
    private List<PostManRequestQuery> query;

    public List<String> getHost() {
        return host;
    }

    public PostManRequestUrl setHost(List<String> host) {
        this.host = host;
        return this;
    }

    public String getPort() {
        return port;
    }

    public PostManRequestUrl setPort(String port) {
        this.port = port;
        return this;
    }

    public List<String> getPath() {
        return path;
    }

    public PostManRequestUrl setPath(List<String> path) {
        this.path = path;
        return this;
    }

    public List<PostManRequestQuery> getQuery() {
        return query;
    }

    public PostManRequestUrl setQuery(List<PostManRequestQuery> query) {
        this.query = query;
        return this;
    }
}
