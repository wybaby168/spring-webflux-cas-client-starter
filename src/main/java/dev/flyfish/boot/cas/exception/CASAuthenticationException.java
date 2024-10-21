package dev.flyfish.boot.cas.exception;

public class CASAuthenticationException extends Exception {

    public CASAuthenticationException(String string) {
        super(string);
    }

    public CASAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
