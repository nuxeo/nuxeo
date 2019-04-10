/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Exception thrown by {@link FileSystemChangeFinder} when too many document changes are found in the audit logs.
 * 
 * @author Antoine Taillefer
 */
public class TooManyChangesException extends ClientException {

    private static final long serialVersionUID = 3125077418830178767L;

    public TooManyChangesException(String message) {
        super(message);
    }

    public TooManyChangesException(String message, Throwable cause) {
        super(message, cause);
    }

    public TooManyChangesException(Throwable cause) {
        super(cause);
    }

}
