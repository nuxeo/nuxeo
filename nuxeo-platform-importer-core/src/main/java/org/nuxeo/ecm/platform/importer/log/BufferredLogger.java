package org.nuxeo.ecm.platform.importer.log;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;

public class BufferredLogger extends BasicLogger {

    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected static int DEFAULT_LOG_BUFFER_LENGTH = 50;

    protected Integer bufferSize= null;

    protected LinkedList<String> logStack = new LinkedList<String>();

    public BufferredLogger(Log javaLogger) {
        super(javaLogger);
    }

    public BufferredLogger(Log javaLogger, int bufferSize) {
        super(javaLogger);
        this.bufferSize = bufferSize;
    }


    protected int getMaxStackLen() {
        if (bufferSize==null) {
            bufferSize = DEFAULT_LOG_BUFFER_LENGTH;
        }
        return bufferSize;
    }

    protected void logInStack(String level, String message) {
        if (!bufferActive) {
            return;
        }
        lock.writeLock().lock();
        try {
            logStack.add(level + " : " + message);
            if (logStack.size() > getMaxStackLen()) {
                logStack.remove();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getLoggerBuffer(String sep) {
        StringBuffer sb = new StringBuffer();

        lock.readLock().lock();
        try {
            for (String line : logStack) {
                sb.append(line);
                sb.append(sep);
            }
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void info(String message) {
        super.info(message);
        if (bufferActive) {
            logInStack("INFO", message);
        }
    }

    @Override
    public void warn(String message) {
        super.warn(message);
        if (bufferActive) {
            logInStack("WARN", message);
        }
    }

    @Override
    public void debug(String message) {
        super.debug(message);
        if (bufferActive) {
            logInStack("DEBUG", message);
        }
    }

    @Override
    public void debug(String message, Throwable t) {
        super.debug(message, t);
        if (bufferActive) {
            logInStack("DEBUG", message);
            logInStack("=>ERR", t.getClass().getSimpleName() + ":" + t.getMessage());
        }
    }

    @Override
    public void error(String message) {
        super.error(message);
        if (bufferActive) {
            logInStack("ERROR", message);
        }
    }

    @Override
    public void error(String message, Throwable t) {
        super.error(message, t);
        if (bufferActive) {
            logInStack("ERROR", message);
            logInStack("=>ERR", t.getClass().getSimpleName() + ":" + t.getMessage());
        }
    }


}
