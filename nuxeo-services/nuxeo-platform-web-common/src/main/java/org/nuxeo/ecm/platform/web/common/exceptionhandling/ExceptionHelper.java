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

import org.jboss.remoting.transport.coyote.ClientAbortException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;

public class ExceptionHelper {

    private ExceptionHelper() {
    }

    public static Boolean isSecurityError(Throwable t) {
        if (t instanceof DocumentSecurityException) {
            return true;
        } else if (t.getCause() instanceof DocumentSecurityException) {
            return true;
        } else if (t.getCause() instanceof SecurityException) {
            return true;
        } else if (t.getMessage() != null
                && t.getMessage().contains("java.lang.SecurityException")) {
            return true;
        }

        return false;
    }

    public static Boolean isClientAbortError(Throwable t) {
        if (t instanceof ClientAbortException || t instanceof SocketException) {
            return true;
        } else if (t.getCause() instanceof ClientAbortException
                || t.getCause() instanceof SocketException) {
            return true;
        }

        return false;
    }

}
