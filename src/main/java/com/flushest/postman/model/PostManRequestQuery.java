package com.flushest.postman.model;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
public class PostManRequestQuery {
    private String key;
    private String value;
    private String description;

    public String getKey() {
        return key;
    }

    public PostManRequestQuery setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public PostManRequestQuery setValue(String value) {
        this.value = value;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PostManRequestQuery setDescription(String description) {
        this.description = description;
        return this;
    }
}
