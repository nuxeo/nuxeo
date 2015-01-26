/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.web.common.exceptionhandling;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentSecurityException;

public class ExceptionHelper {

    private static final Log log = LogFactory.getLog(ExceptionHelper.class);

    // tomcat catalina
    private static final String CLIENT_ABORT_EXCEPTION = "ClientAbortException";

    // jetty (with CamelCase "Eof")
    private static final String EOF_EXCEPTION = "EofException";

    private ExceptionHelper() {
    }

    public static Throwable unwrapException(Throwable t) {
        Throwable cause = null;
        if (t instanceof ServletException) {
            cause = ((ServletException) t).getRootCause();
        } else if (t instanceof Exception) {
            cause = t.getCause();
        }

        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

    public static List<String> possibleSecurityErrorMessages = Arrays.asList("java.lang.SecurityException",
            DocumentSecurityException.class.getName(), SecurityException.class.getName());

    public static boolean isSecurityError(Throwable t) {
        if (t == null) {
            return false;
        } else if (t instanceof DocumentSecurityException || t.getCause() instanceof DocumentSecurityException
                || t.getCause() instanceof SecurityException) {
            return true;
        } else if (t.getMessage() != null) {
            String message = t.getMessage();
            for (String errorMessage : possibleSecurityErrorMessages) {
                if (message.contains(errorMessage)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isClientAbortError(Throwable t) {
        int loops = 20; // no infinite loop
        while (t != null && loops > 0) {
            if (t instanceof IOException) {
                // handle all IOException that are ClientAbortException by looking
                // at their class name since the package name is not the same for
                // jboss, glassfish, tomcat and jetty and we don't want to add
                // implementation specific build dependencies to this project
                String name = t.getClass().getSimpleName();
                if (CLIENT_ABORT_EXCEPTION.equals(name) || EOF_EXCEPTION.equals(name)) {
                    return true;
                }
            }
            loops--;
            t = t.getCause();
        }
        return false;
    }

    public static void logClientAbort(Exception e) {
        log.warn("Client disconnected: " + unwrapException(e).getMessage());
    }

}
