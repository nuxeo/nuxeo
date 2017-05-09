/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.common.logging;

import java.lang.reflect.Field;

import org.apache.log4j.LogManager;
import org.apache.log4j.helpers.FileWatchdog;
import org.apache.log4j.xml.DOMConfigurator;

class Log4jWatchdog extends FileWatchdog implements Log4jWatchdogHandle {

    protected Log4jWatchdog(String filename) {
        super(filename);
        setName("Nuxeo Log4J Watchdog");
    }

    @Override
    protected void doOnChange() {
        configure();
    }

    protected void configure() {
        new DOMConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
    }

    @Override
    public void cancel() {
        try {
            Field field = FileWatchdog.class.getDeclaredField("interrupted");
            field.setAccessible(true);
            field.set(this, true);
        } catch (ReflectiveOperationException cause) {
            throw new RuntimeException("Cannot cancel log4j watchdog", cause);
        }
    }

    public static Log4jWatchdogHandle watch(String filename, long delay) {
        Log4jWatchdog wdog = new Log4jWatchdog(filename);
        wdog.setDelay(delay);
        wdog.start();
        return wdog;
    }
}