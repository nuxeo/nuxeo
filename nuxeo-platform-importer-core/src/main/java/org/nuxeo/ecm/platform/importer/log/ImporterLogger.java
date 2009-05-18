package org.nuxeo.ecm.platform.importer.log;

public interface ImporterLogger {

	public void info(String message);

	public void warn(String message);

	public void debug(String message);

	public void debug(String message, Throwable t);

	public void error(String message);

	public void error(String message, Throwable t);

	public String getLoggerBuffer(String sep);

	public String getLoggerBuffer();

}