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
 * $Id$
 */

package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * An exception thrown when a communication error occurs during an operation
 * on an NXDirectory.
 *
 * @author glefter@nuxeo.com
 */
public class DirectoryException extends ClientException {

    private static final long serialVersionUID = -2358123805931727792L;

    public DirectoryException() {
    }

    public DirectoryException(String message, Throwable th) {
        super(message, th);
    }

    public DirectoryException(String message) {
        super(message);
    }

    public DirectoryException(Throwable th) {
        super(th);
    }

    /**
     * Wraps the received exception into a {@link ClientException}.
     */
    public static DirectoryException wrap(Throwable exception) {
        DirectoryException clientException;

        if (null == exception) {
            clientException = new DirectoryException(
                    "Root exception was null. Pls check your code.");
        } else {
            if (exception instanceof DirectoryException) {
                clientException = (DirectoryException) exception;
            } else {
                if (exception instanceof Error) {
                    clientException = new DirectoryException(
                            "An ERROR type of exception occurred. This will most likely kill your session/application",
                            exception);
                } else {
                    if (exception instanceof RuntimeException) {
                        clientException = new DirectoryException(
                                "Runtime exception was raised. Wrapping now...",
                                exception);
                    } else {
                        clientException = new DirectoryException(
                                "Unwrapped application exception was raised. Wrapping now...",
                                exception);
                    }
                }
            }
        }
        return clientException;
    }

}
