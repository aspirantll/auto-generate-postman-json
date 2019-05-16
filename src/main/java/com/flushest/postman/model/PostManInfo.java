package com.flushest.postman.model;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
public class PostManInfo {
    private String _postman_id;
    private String name;
    private String description;
    private String schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";


    public String get_postman_id() {
        return _postman_id;
    }

    public PostManInfo set_postman_id(String _postman_id) {
        this._postman_id = _postman_id;
        return this;
    }

    public String getName() {
        return name;
    }

    public PostManInfo setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PostManInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getSchema() {
        return schema;
    }

    public PostManInfo setSchema(String schema) {
        this.schema = schema;
        return this;
    }
}
