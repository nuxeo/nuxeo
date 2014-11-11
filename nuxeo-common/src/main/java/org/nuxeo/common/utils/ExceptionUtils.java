/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
