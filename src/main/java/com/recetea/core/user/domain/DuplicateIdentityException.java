package com.recetea.core.user.domain;

public class DuplicateIdentityException extends RuntimeException {

    public DuplicateIdentityException(String message) {
        super(message);
    }
}
