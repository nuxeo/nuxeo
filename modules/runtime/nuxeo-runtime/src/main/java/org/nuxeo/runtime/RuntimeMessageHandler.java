/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime;

import java.util.List;

/**
 * Handles runtime message.
 *
 * @since 9.10
 */
public interface RuntimeMessageHandler {

    /**
     * Warning messages don't block server startup.
     */
    void addWarning(String message);

    /**
     * @return an unmodifiable {@link List} of warning messages
     */
    List<String> getWarnings();

    /**
     * Add new error.
     * <p />
     * Error messages block server startup in strict mode.
     */
    void addError(String message);

    /**
     * @return an unmodifiable {@link List} of error messages
     */
    List<String> getErrors();

}
