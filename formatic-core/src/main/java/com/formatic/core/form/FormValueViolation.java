package com.formatic.core.form;

/** A submission failure, without retaining or exposing the submitted value. */
public record FormValueViolation(String path, String code, String message) {}
