package com.formatic.annotation;

public interface CommonFieldAttributes {
    String name();
    String label();
    boolean required();
    boolean disabled();
    String[] htmlAttributes();
}
