package com.flushest.postman.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
public class PostManItem {
    private String name;
    private PostManRequest request;
    private List response;

    public PostManItem() {
        response = new ArrayList();
    }

    public String getName() {
        return name;
    }

    public PostManItem setName(String name) {
        this.name = name;
        return this;
    }

    public PostManRequest getRequest() {
        return request;
    }

    public PostManItem setRequest(PostManRequest request) {
        this.request = request;
        return this;
    }

    public List getResponse() {
        return response;
    }

    public PostManItem setResponse(List response) {
        this.response = response;
        return this;
    }
}
