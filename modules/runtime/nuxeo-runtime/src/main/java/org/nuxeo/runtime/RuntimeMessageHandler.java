/*
 * (C) Copyright 2017-2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.List;
import java.util.function.Predicate;

import org.nuxeo.runtime.RuntimeMessage.Level;

/**
 * Handles runtime message.
 *
 * @since 9.10
 */
public interface RuntimeMessageHandler {

    /**
     * Adds the following message.
     *
     * @since 11.3
     */
    void addMessage(Level level, String message);

    /**
     * Adds the following message.
     *
     * @since 11.3
     */
    void addMessage(RuntimeMessage message);

    /**
     * Returns all messages strings, filtered by given level.
     *
     * @since 11.3
     */
    List<String> getMessages(Level level);

    /**
     * Returns all messages strings, filtered by given predicate.
     *
     * @since 11.3
     */
    List<String> getMessages(Predicate<RuntimeMessage> predicate);

    /**
     * Returns all messages, filtered by given level.
     *
     * @since 11.3
     */
    List<RuntimeMessage> getRuntimeMessages(Level level);

    /**
     * Returns all messages, filtered by given predicate.
     *
     * @since 11.3
     */
    List<RuntimeMessage> getRuntimeMessages(Predicate<RuntimeMessage> predicate);

    /**
     * Warning messages don't block server startup.
     *
     * @deprecated since 11.3, use {@link #addMessage(Level, String)} instead.
     */
    @Deprecated
    void addWarning(String message);

    /**
     * @return an unmodifiable {@link List} of warning messages
     * @deprecated since 11.3, use {@link #getMessages(Level)} instead
     */
    @Deprecated
    List<String> getWarnings();

    /**
     * Add new error.
     * <p />
     * Error messages block server startup in strict mode.
     *
     * @deprecated since 11.3, use {@link #addMessage(Level, String)} instead.
     */
    @Deprecated
    void addError(String message);

    /**
     * @return an unmodifiable {@link List} of error messages
     * @deprecated since 11.3, use {@link #getMessages(Level)} instead
     */
    @Deprecated
    List<String> getErrors();

}
