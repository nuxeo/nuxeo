/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.core.api;

/**
 * An application that should rollback the transaction if any.
 *
 * In an ejb container, the nuxeo-core-api module is package with the
 * nuxeo-core-facade module. The facade has a ejb-jar.xml that declare this
 * exception has rollback=true.
 *
 * @author arussel
 *
 */
public class RollbackClientException extends ClientException {

    private static final long serialVersionUID = 1L;

    public RollbackClientException() {
    }

    public RollbackClientException(ClientException cause) {
        super(cause);
    }

    public RollbackClientException(String message, ClientException cause) {
        super(message, cause);
    }

    public RollbackClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public RollbackClientException(String message) {
        super(message);
    }

    public RollbackClientException(Throwable cause) {
        super(cause);
    }

}
