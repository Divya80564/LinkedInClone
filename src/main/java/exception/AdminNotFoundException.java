// .\exception\AdminNotFoundException.java
package com.divya.linkedinclone.exception;

public class AdminNotFoundException extends RuntimeException {
    public AdminNotFoundException(String message) {
        super(message);
    }
}