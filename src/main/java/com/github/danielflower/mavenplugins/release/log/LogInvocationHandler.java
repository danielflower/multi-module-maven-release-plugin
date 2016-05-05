package com.github.danielflower.mavenplugins.release.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;

@Named
@Singleton
final class LogInvocationHandler implements InvocationHandler, LogHolder {
	private Log delegate;

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		if (delegate == null) {
			throw new IllegalStateException("Delegate logger has not been set!");
		}
		return method.invoke(delegate, args);
	}

	@Override
	public void setLog(final Log log) {
		delegate = log;
	}
}
