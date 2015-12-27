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
 */
package org.nuxeo.ecm.automation.core.util;

/**
 * Enum describing the format to use when marshaling a date into JSON.
 *
 * @since 7.1
 */
public enum DateTimeFormat {

    /**
     * Marshals the date as a W3C date (ISO 8601).
     * <p>
     * Example: {@code 2011-10-23T12:00:00.00Z}.
     */
    W3C,

    /**
     * Marshals the date as a number of milliseconds since epoch.
     */
    TIME_IN_MILLIS;

}
