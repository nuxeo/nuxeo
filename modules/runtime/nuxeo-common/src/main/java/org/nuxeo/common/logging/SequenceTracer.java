/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Delbosc Benoit
 */
package org.nuxeo.common.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper to log information that can be displayed using plantuml to render sequence UML diagram.
 *
 * @since 8.1
 */
public class SequenceTracer {

    private static final Log log = LogFactory.getLog(SequenceTracer.class);
    private static final String PREFIX = "@@ ";
    private static final String DEFAULT_COLOR = "#white";
    private static final int MESSAGE_MAX_LEN = 250;

    // Utility class.
    private SequenceTracer() {
    }

    /**
     * Mark an event.
     */
    public static void mark(String message) {
        if (!log.isDebugEnabled()) {
            return;
        }
        final String tn = getThreadName();
        log.debug(PREFIX + tn + " -> " + tn + ": " + message);
    }

    /**
     * Mark the beginning of an action
     */
    public static void start(String message) {
        start(message, DEFAULT_COLOR);
    }

    /**
     * Mark the beginning of an action
     */
    public static void start(String message, String color) {
        if (!log.isDebugEnabled()) {
            return;
        }
        final String tn = getThreadName();
        log.debug(PREFIX + tn + " -> " + tn + ": " + format(message) + "\n" + PREFIX + "activate " + tn + " " +
                color);
    }

    /**
     * Mark the beginning of an action initiated by the caller.
     */
    public static void startFrom(final String callerThread, final String message) {
        startFrom(callerThread, message, DEFAULT_COLOR);
    }

    /**
     * Mark the beginning of an action initiated by the caller.
     */
    public static void startFrom(final String callerThread, final String message, final String color) {
        if (!log.isDebugEnabled()) {
            return;
        }
        final String tn = getThreadName();
        log.debug(PREFIX + sanitize(callerThread) + " o--> " + tn + ": Initiate\n" + PREFIX
                + tn + " -> " + tn + ": " + format(message) + "\n" + PREFIX
                + "activate " + tn + " " + color);
    }

    private static String format(String message) {
        String ret = sanitize(message).replace(", ", ",\\n");
        ret = insertNewLine(ret);
        return ret;
    }

    private static String sanitize(String message) {
        String ret = message.replace("-", "_").replace(":", "_");
        if (ret.length() > MESSAGE_MAX_LEN) {
            ret = ret.substring(0, MESSAGE_MAX_LEN) + "...";
        }
        return ret;
    }

    private static String insertNewLine(String message) {
        return String.join("\\n", message.split("(?<=\\G.{40})"));
    }

    /**
     * Mark the end of the previous action.
     */
    public static void stop(String message) {
        if (!log.isDebugEnabled()) {
            return;
        }
        final String tn = getThreadName();
        log.debug(PREFIX + tn + " -> " + tn + ": " + format(message) + "\n" + PREFIX + "deactivate " + tn);
    }

    /**
     * Mark the last action as failure
     */
    public static void destroy(String message) {
        if (!log.isDebugEnabled()) {
            return;
        }
        final String tn = getThreadName();
        log.debug(PREFIX + tn + " -> " + tn + ": " + format(message) + "\n" + PREFIX + "destroy " + tn);
    }

    /**
     * Add a note on the current thread
     */
    public static void addNote(String message) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(PREFIX + "note right of " + getThreadName() + ": " + message);
    }

    /**
     * Link from source to current thread.
     */
    public static void addRelation(String source, String message) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(PREFIX + source + " --> " + getThreadName() + ": " + format(message));
    }

    /**
     * Get the thread name sanitized for plantuml
     */
    public static String getThreadName() {
        return sanitize(Thread.currentThread().getName());
    }

    public static boolean isEnabled() {
        return log.isDebugEnabled();
    }
}
