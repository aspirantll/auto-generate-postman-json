package com.flushest.postman;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
public class RequestMap {
    private String[] path;
    private RequestMethod method;

    public String[] getPath() {
        return path;
    }

    public RequestMap setPath(String[] path) {
        this.path = path;
        return this;
    }

    public RequestMethod getMethod() {
        return method;
    }

    public RequestMap setMethod(RequestMethod method) {
        this.method = method;
        return this;
    }
}
