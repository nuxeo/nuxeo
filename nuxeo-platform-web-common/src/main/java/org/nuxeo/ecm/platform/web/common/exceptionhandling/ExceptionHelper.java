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
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.exceptionhandling;

import java.net.SocketException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import org.jboss.remoting.transport.coyote.ClientAbortException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;

public class ExceptionHelper {

    private ExceptionHelper() {
    }

    public static Throwable unwrapException(Throwable t) {
        Throwable cause = null;

        if (t instanceof ServletException) {
            cause = ((ServletException) t).getRootCause();
        } else if (t instanceof ClientException) {
            cause = t.getCause();
        } else if (t instanceof Exception) {
            cause = t.getCause();
        }

        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

    public static List<String> possibleSecurityErrorMessages = Arrays.asList(
            "java.lang.SecurityException",
            DocumentSecurityException.class.getName(),
            SecurityException.class.getName());

    public static boolean isSecurityError(Throwable t) {
        if (t instanceof DocumentSecurityException) {
            return true;
        } else if (t.getCause() instanceof DocumentSecurityException) {
            return true;
        } else if (t.getCause() instanceof SecurityException) {
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
        if (t instanceof ClientAbortException || t instanceof SocketException) {
            return true;
        } else if (t.getCause() instanceof ClientAbortException
                || t.getCause() instanceof SocketException) {
            return true;
        } else if (t != null) {
            // handle all IOException that are ClientAbortException by looking
            // at their class name since the package name is not the same for
            // jboss, glassfish, tomcat and jetty and we don't want to add
            // implementation specific build dependencies to this project
            if (ClientAbortException.class.getSimpleName().equals(
                    t.getClass().getSimpleName())) {
                return true;
            }
            Throwable cause = t.getCause();
            if (cause != null
                    && ClientAbortException.class.getSimpleName().equals(
                            cause.getClass().getSimpleName())) {
                return true;
            }
        }

        return false;
    }
}
