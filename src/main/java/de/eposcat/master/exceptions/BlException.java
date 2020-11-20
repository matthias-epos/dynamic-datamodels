package de.eposcat.master.exceptions;

/**
 * An exception class used when the database api is used incorrectly. The name is short for business logic exception
 */
public class BlException extends RuntimeException{
    public BlException(String message){
        super(message);
    }
}
