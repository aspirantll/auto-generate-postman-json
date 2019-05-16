package com.flushest.postman.model;

import java.util.List;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc postman集合
 */
public class PostManCollection {
    private PostManInfo info;
    private List<PostManItem> item;

    public PostManInfo getInfo() {
        return info;
    }

    public PostManCollection setInfo(PostManInfo info) {
        this.info = info;
        return this;
    }

    public List<PostManItem> getItem() {
        return item;
    }

    public PostManCollection setItem(List<PostManItem> item) {
        this.item = item;
        return this;
    }
}
