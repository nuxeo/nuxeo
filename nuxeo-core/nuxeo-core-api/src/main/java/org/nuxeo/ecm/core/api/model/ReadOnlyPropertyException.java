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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model;

import org.nuxeo.ecm.core.api.PropertyException;

/**
 * Exception thrown when attempting to write to a read-only property.
 */
public class ReadOnlyPropertyException extends PropertyException {

    private static final long serialVersionUID = 1L;

    public ReadOnlyPropertyException() {
        super();
    }

    public ReadOnlyPropertyException(String message) {
        super(message);
    }

    public ReadOnlyPropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadOnlyPropertyException(Throwable cause) {
        super(cause);
    }

}
