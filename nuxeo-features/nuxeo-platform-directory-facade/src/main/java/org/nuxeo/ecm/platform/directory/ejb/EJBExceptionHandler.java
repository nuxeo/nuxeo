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

package org.nuxeo.ecm.platform.directory.ejb;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.DirectoryException;

/**
 * Offers utility methods to handle exceptions.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
public class EJBExceptionHandler implements Serializable {

    private static final long serialVersionUID = 3571163516248088734L;

    private static final Log log = LogFactory.getLog(EJBExceptionHandler.class);

    /**
     * Wraps the received exception into a {@link ClientException}.
     *
     * @param exception
     * @return
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public static DirectoryException wrapException(Throwable exception) {
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

        log.info("Exception wrapped...");
        return clientException;
    }
}
