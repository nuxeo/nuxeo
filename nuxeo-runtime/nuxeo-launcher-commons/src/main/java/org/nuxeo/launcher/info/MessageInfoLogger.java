/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     mguillaume
 */

package org.nuxeo.launcher.info;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.impl.SimpleLog;

public class MessageInfoLogger {

    private List<MessageInfo> messages = new ArrayList<>();

    public List<MessageInfo> getMessages() {
        return messages;
    }

    public void printMessages() {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (MessageInfo message : messages) {
            System.out.println("[" + dateFormat.format(message.time) + "] " + message.level + " " + message.message);
        }
    }

    public void log(String msg, int level) {
        MessageInfo message = new MessageInfo();
        message.time = new Date();
        message.level = level;
        message.message = msg;
        messages.add(message);
    }

    public void debug(Object... args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                debug((String) arg);
            } else if (arg instanceof Throwable) {
                Writer stringWriter = new StringWriter();
                PrintWriter stackWriter = new PrintWriter(stringWriter);
                ((Throwable) arg).printStackTrace(stackWriter);
                debug(stringWriter.toString());
            } else {
                debug(arg.toString());
            }
        }
    }

    public void debug(String msg) {
        log(msg, SimpleLog.LOG_LEVEL_DEBUG);
    }

    public void info(Object... args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                info((String) arg);
            } else if (arg instanceof Throwable) {
                Writer stringWriter = new StringWriter();
                PrintWriter stackWriter = new PrintWriter(stringWriter);
                ((Throwable) arg).printStackTrace(stackWriter);
                info(stringWriter.toString());
            } else {
                info(arg.toString());
            }
        }
    }

    public void info(String msg) {
        log(msg, SimpleLog.LOG_LEVEL_INFO);
    }

    public void warn(Object... args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                warn((String) arg);
            } else if (arg instanceof Throwable) {
                Writer stringWriter = new StringWriter();
                PrintWriter stackWriter = new PrintWriter(stringWriter);
                ((Throwable) arg).printStackTrace(stackWriter);
                warn(stringWriter.toString());
            } else {
                warn(arg.toString());
            }
        }
    }

    public void warn(String msg) {
        log(msg, SimpleLog.LOG_LEVEL_WARN);
    }

    public void error(Object... args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                error((String) arg);
            } else if (arg instanceof Throwable) {
                Writer stringWriter = new StringWriter();
                PrintWriter stackWriter = new PrintWriter(stringWriter);
                ((Throwable) arg).printStackTrace(stackWriter);
                error(stringWriter.toString());
            } else {
                error(arg.toString());
            }
        }
    }

    public void error(String msg) {
        log(msg, SimpleLog.LOG_LEVEL_ERROR);
    }

}
