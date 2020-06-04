/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.launcher.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * @since 11.1
 * @apiNote it was an inner class in {@link ConfigurationGeneratorTest}
 */
public class LogCaptureAppender extends AbstractAppender {

    protected final List<String> messages = new ArrayList<>();

    protected final Level level;

    protected final String loggerName;

    public LogCaptureAppender(Level level, Class<?> loggerClass) {
        super(LogCaptureAppender.class.getName(), null, null, true, new Property[0]);
        this.level = level;
        this.loggerName = loggerClass.getName();
    }

    @Override
    public void append(LogEvent event) {
        if (loggerName.equals(event.getLoggerName()) && level.equals(event.getLevel())) {
            messages.add(event.getMessage().getFormattedMessage());
        }
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public String get(int i) {
        return messages.get(i);
    }

    public int size() {
        return messages.size();
    }

    public void clear() {
        messages.clear();
    }

}
