/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.common.utils;

/**
 * Provides utility methods for manipulating and examining
 * exceptions in a generic way.
 *
 * @author DM
 */
public final class ExceptionUtils {

    // This is an utility class.
    private ExceptionUtils() {
    }

    /**
     * Gets the root cause of the given <code>Throwable</code>.
     * <p>
     * This method walks through the exception chain up to the root of the
     * exceptions tree using {@link Throwable#getCause()}, and returns the root
     * exception.
     *
     * @param throwable
     *            the throwable to get the root cause for, may be null - this is
     *            to avoid throwing other un-interesting exception when handling
     *            a business-important exception
     * @return the root cause of the <code>Throwable</code>,
     *         <code>null</code> if none found or null throwable input
     */
    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        if (throwable != null) {
            cause = throwable.getCause();
            while ((throwable = cause.getCause()) != null) {
                cause = throwable;
            }
        }

        return cause;
    }

}
