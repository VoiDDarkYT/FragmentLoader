package dev.fragmentcode.transformer.mixin;

public final class MixinException extends RuntimeException {

    public MixinException(String message) {
        super(message);
    }

    public MixinException(String message, Throwable cause) {
        super(message, cause);
    }

}
