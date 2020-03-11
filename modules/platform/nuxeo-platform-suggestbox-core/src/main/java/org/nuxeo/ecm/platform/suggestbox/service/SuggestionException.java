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
 * Exception raised when a suggester cannot perform it's suggestion due to inconsistent configuration or problem when
 * calling a backend service: in that case the backend service exception should be wrapped as the cause.
 */
public class SuggestionException extends Exception {

    private static final long serialVersionUID = 1L;

    public SuggestionException(String msg) {
        super(msg);
    }

    public SuggestionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SuggestionException(Throwable cause) {
        super(cause);
    }
}
