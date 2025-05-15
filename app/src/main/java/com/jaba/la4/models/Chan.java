package com.jaba.la4.models;

public class Chan {
    private final String name;
    private final String url;
    private final String icon;
    private final int index;

    public Chan(String name, String url, String icon, int index) {
        this.name = name;
        this.url = url;
        this.icon = icon;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getIcon() {
        return icon;
    }

    public int getIndex() {
        return index;
    }
}
