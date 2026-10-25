package com.sep.sep_backend.cost;

/**
 * Thrown when a scanned document contains no usable cost information.
 */
public class NoCostsFoundException extends RuntimeException {

    public NoCostsFoundException(String message) {
        super(message);
    }
}
