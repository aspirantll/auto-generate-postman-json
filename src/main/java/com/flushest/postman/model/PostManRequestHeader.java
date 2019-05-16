package com.flushest.postman.model;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
public class PostManRequestHeader {
    private String key;
    private String name;
    private String value;
    private String type;

    public String getKey() {
        return key;
    }

    public PostManRequestHeader setKey(String key) {
        this.key = key;
        return this;
    }

    public String getName() {
        return name;
    }

    public PostManRequestHeader setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public PostManRequestHeader setValue(String value) {
        this.value = value;
        return this;
    }

    public String getType() {
        return type;
    }

    public PostManRequestHeader setType(String type) {
        this.type = type;
        return this;
    }
}
