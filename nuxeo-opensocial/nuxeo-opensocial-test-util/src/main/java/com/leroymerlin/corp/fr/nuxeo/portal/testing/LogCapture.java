package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.io.StringWriter;
import java.io.Writer;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;

public class LogCapture {

	private StringWriter logWriter;
	private Logger rootLogger;

	public LogCapture() {
		logWriter = new StringWriter();
		rootLogger = Logger.getRootLogger();
		logToString(logWriter);
	}

	private void logToString(Writer logWriter) {
		rootLogger.removeAllAppenders();
		Appender stringAppender = new WriterAppender(new SimpleLayout(),
				logWriter);
		rootLogger.addAppender(stringAppender);
	}

	public LogCapture debug() {
		rootLogger.setLevel(Level.DEBUG);
		return this;
	}

	public String content() {
		return logWriter.toString();
	}


}
