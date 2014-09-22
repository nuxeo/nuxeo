/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * @since 5.9.6
 */
public class Assert {

    /**
     * Protect constructor since it is a static only class
     */
    protected Assert() {
    }

    /**
     * Asserts that two strings are equal even if their EOL are different. If
     * they are not, an {@link AssertionError} is thrown with the given message.
     * If <code>expected</code> and <code>actual</code> are <code>null</code>,
     * they are considered equal.
     *
     * @param expected expected String with Windows or Unix like EOL
     * @param actual actual String with Windows or Unix like EOL
     * @see FileUtils#areFilesContentEquals(String, String)
     */
    static public void assertFilesContentEquals(String expected, String actual) {
        assertFilesContentEquals(null, expected, actual);
    }

    /**
     * Asserts that two strings are equal even if their EOL are different. If
     * they are not, an {@link AssertionError} is thrown with the given message.
     * If <code>expected</code> and <code>actual</code> are <code>null</code>,
     * they are considered equal.
     *
     * @param message
     *            the identifying message for the {@link AssertionError} (
     *            <code>null</code> okay)
     * @param expected expected String with Windows or Unix like EOL
     * @param actual actual String with Windows or Unix like EOL
     * @see FileUtils#areFilesContentEquals(String, String)
     */
    static public void assertFilesContentEquals(String message,
            String expected, String actual) {
        if (FileUtils.areFilesContentEquals(expected, actual)) {
            return;
        } else {
            String cleanMessage = message == null ? "" : message;
            throw new ComparisonFailure(cleanMessage, expected, actual);
        }
    }
}
