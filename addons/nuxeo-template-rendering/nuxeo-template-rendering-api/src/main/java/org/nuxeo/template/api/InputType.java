/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.template.api;

import java.util.Date;
import java.util.EnumSet;

/**
 * Enum for types of {@link TemplateInput}
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public enum InputType {

    StringValue(String.class.getSimpleName()), //
    BooleanValue(Boolean.class.getSimpleName()), //
    DateValue(Date.class.getSimpleName()), //
    DocumentProperty("source"), //
    PictureProperty("picture"), //
    Content("content");

    private final String value;

    InputType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static InputType getByValue(String value) {
        InputType returnValue = null;
        for (final InputType element : EnumSet.allOf(InputType.class)) {
            if (element.toString().equals(value)) {
                returnValue = element;
            }
        }
        return returnValue;
    }
}
