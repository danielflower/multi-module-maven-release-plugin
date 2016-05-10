package com.github.danielflower.mavenplugins.release.log;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = Log.class)
final class LogProvider implements Log {

	@Requirement(role = LogHolder.class)
	private LogHolder holder;

	void setLogHolder(final LogHolder holder) {
		this.holder = holder;
	}

	private Log getLog() {
		return holder.getLog();
	}

	@Override
	public boolean isDebugEnabled() {
		return getLog().isDebugEnabled();
	}

	@Override
	public void debug(final CharSequence content) {
		getLog().debug(content);
	}

	@Override
	public void debug(final CharSequence content, final Throwable error) {
		getLog().debug(content, error);
	}

	@Override
	public void debug(final Throwable error) {
		getLog().debug(error);
	}

	@Override
	public boolean isInfoEnabled() {
		return getLog().isInfoEnabled();
	}

	@Override
	public void info(final CharSequence content) {
		getLog().info(content);
	}

	@Override
	public void info(final CharSequence content, final Throwable error) {
		getLog().info(content, error);
	}

	@Override
	public void info(final Throwable error) {
		getLog().info(error);
	}

	@Override
	public boolean isWarnEnabled() {
		return getLog().isWarnEnabled();
	}

	@Override
	public void warn(final CharSequence content) {
		getLog().warn(content);
	}

	@Override
	public void warn(final CharSequence content, final Throwable error) {
		getLog().warn(content, error);
	}

	@Override
	public void warn(final Throwable error) {
		getLog().warn(error);
	}

	@Override
	public boolean isErrorEnabled() {
		return getLog().isErrorEnabled();
	}

	@Override
	public void error(final CharSequence content) {
		getLog().error(content);
	}

	@Override
	public void error(final CharSequence content, final Throwable error) {
		getLog().error(content, error);
	}

	@Override
	public void error(final Throwable error) {
		getLog().error(error);
	}
}
