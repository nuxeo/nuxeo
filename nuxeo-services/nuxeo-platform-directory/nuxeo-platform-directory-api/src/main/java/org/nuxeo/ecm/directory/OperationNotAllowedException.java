/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.RecoverableClientException;

/**
 * Exception thrown when an operation is not allowed on a given directory entry.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class OperationNotAllowedException extends RecoverableClientException {

    private static final long serialVersionUID = -1L;

    public OperationNotAllowedException(String message, String localizedMessage,
                                        String[] params) {
        super(message, localizedMessage, params);
    }

    public OperationNotAllowedException(String message, String localizedMessage,
                                        String[] params, Throwable cause) {
        super(message, localizedMessage, params, cause);
    }

}
