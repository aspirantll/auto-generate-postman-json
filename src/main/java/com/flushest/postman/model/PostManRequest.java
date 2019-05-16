package com.flushest.postman.model;

import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
public class PostManRequest {
    private RequestMethod method;
    private List<PostManRequestHeader> header;
    private Map<String, Object> body;
    private PostManRequestUrl url;

    public RequestMethod getMethod() {
        return method;
    }

    public PostManRequest setMethod(RequestMethod method) {
        this.method = method;
        return this;
    }

    public List<PostManRequestHeader> getHeader() {
        return header;
    }

    public PostManRequest setHeader(List<PostManRequestHeader> header) {
        this.header = header;
        return this;
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public PostManRequest setBody(Map<String, Object> body) {
        this.body = body;
        return this;
    }

    public PostManRequestUrl getUrl() {
        return url;
    }

    public PostManRequest setUrl(PostManRequestUrl url) {
        this.url = url;
        return this;
    }
}
