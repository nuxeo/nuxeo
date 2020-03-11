/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

/**
 * Exception thrown when a runtime component related to the SuggestionService fails to initialize due to invalid
 * configuration parameters or missing requirements on the platform.
 */
public class ComponentInitializationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ComponentInitializationException(String msg) {
        super(msg);
    }

    public ComponentInitializationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ComponentInitializationException(Throwable cause) {
        super(cause);
    }
}
