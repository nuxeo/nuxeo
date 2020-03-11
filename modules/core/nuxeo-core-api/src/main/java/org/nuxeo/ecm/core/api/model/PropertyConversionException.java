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

/**
 * Exception thrown when trying to convert a property value to an incompatible type during read or write.
 */
public class PropertyConversionException extends InvalidPropertyValueException {

    private static final long serialVersionUID = 1L;

    public PropertyConversionException() {
        super();
    }

    public PropertyConversionException(String message) {
        super(message);
    }

    public PropertyConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyConversionException(Throwable cause) {
        super(cause);
    }

    public PropertyConversionException(Class<?> fromClass, Class<?> toClass) {
        this("Property Conversion failed from " + fromClass + " to " + toClass);
    }

    public PropertyConversionException(Class<?> fromClass, Class<?> toClass, String message) {
        this("Property Conversion failed from " + fromClass + " to " + toClass + ": " + message);
    }

}
