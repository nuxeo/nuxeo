/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.runtime.test;

import org.junit.ComparisonFailure;

import org.nuxeo.common.utils.FileUtils;

/**
 * Extension to {@link org.junit.Assert}
 *
 * @since 6.0
 */
public class Assert {

    /**
     * Protect constructor since it is a static only class
     */
    protected Assert() {
    }

    /**
     * Asserts that two strings are equal even if their EOL are different. If they are not, an {@link AssertionError} is
     * thrown with the given message. If <code>expected</code> and <code>actual</code> are <code>null</code>, they are
     * considered equal.
     *
     * @param expected expected String with Windows or Unix like EOL
     * @param actual actual String with Windows or Unix like EOL
     * @see FileUtils#areFilesContentEquals(String, String)
     */
    static public void assertFilesContentEquals(String expected, String actual) {
        assertFilesContentEquals(null, expected, actual);
    }

    /**
     * Asserts that two strings are equal even if their EOL are different. If they are not, an {@link AssertionError} is
     * thrown with the given message. If <code>expected</code> and <code>actual</code> are <code>null</code>, they are
     * considered equal.
     *
     * @param message the identifying message for the {@link AssertionError} ( <code>null</code> okay)
     * @param expected expected String with Windows or Unix like EOL
     * @param actual actual String with Windows or Unix like EOL
     * @see FileUtils#areFilesContentEquals(String, String)
     */
    static public void assertFilesContentEquals(String message, String expected, String actual) {
        if (FileUtils.areFilesContentEquals(expected, actual)) {
            return;
        } else {
            String cleanMessage = message == null ? "" : message;
            throw new ComparisonFailure(cleanMessage, expected, actual);
        }
    }
}
