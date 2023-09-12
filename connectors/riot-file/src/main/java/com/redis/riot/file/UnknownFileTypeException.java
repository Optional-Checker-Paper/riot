package com.redis.riot.file;

public class UnknownFileTypeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnknownFileTypeException(String message) {
		super(message);
	}

}