package com.github.danielflower.mavenplugins.release.log;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.lang.reflect.InvocationHandler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;

import com.google.inject.Provider;

@Named
@Singleton
final class LogProvider implements Provider<Log> {
	private final Log log;

	@com.google.inject.Inject // Compatibility: Maven 3.0.1 - 3.2.1
	@Inject // Maven 3.3.0 and greater
	LogProvider(final InvocationHandler handler) {
		log = (Log) newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Log.class }, handler);
	}

	@Override
	public Log get() {
		return log;
	}
}
