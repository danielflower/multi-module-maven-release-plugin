package com.github.danielflower.mavenplugins.release.scm;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.github.danielflower.mavenplugins.release.ValidationException;

final class InstantiationFailedHandler implements InvocationHandler {
	private final ValidationException exception;

	InstantiationFailedHandler(final ValidationException exception) {
		this.exception = exception;
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		throw exception;
	}

}
