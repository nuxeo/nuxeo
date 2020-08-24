/*
 * (C) Copyright 2017-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

/**
 * Represents a message to be held by the Runtime framework.
 * <p>
 * Allows detecting and displaying errors and warnings at server startup and when hot-reloading.
 *
 * @since 11.3
 */
public class RuntimeMessage {

    protected final Level level;

    protected final String message;

    protected final Source source;

    protected final String sourceId;

    public RuntimeMessage(Level level, String message, Source source, String sourceId) {
        this.level = level;
        this.message = message;
        this.source = source;
        this.sourceId = sourceId;
    }

    public RuntimeMessage(Level level, String message) {
        this(level, message, null, null);
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Returns the type of source that produced the message.
     */
    public Source getSource() {
        return source;
    }

    /**
     * Returns a string identifier for the source that produce the message.
     */
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public String toString() {
        return String.format("%s {level=%s, message=%s, source=%s, sourceId=%s}", getClass().getName(), level, message,
                source, sourceId);
    }

    public enum Level {

        ERROR,

        WARNING

    }

    /**
     * The type of source that produced a message.
     * <p>
     * Useful to track errors on components, extension, etc...
     */
    public enum Source {

        BUNDLE, //
        COMPONENT, //
        EXTENSION, //
        CODE,

    }

}
