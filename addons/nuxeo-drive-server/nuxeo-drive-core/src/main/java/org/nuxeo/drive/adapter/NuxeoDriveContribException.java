/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.adapter;

import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Exception raised when no contributions to the {@link FileSystemItemAdapterService} are found or the component is not
 * ready, ie. application has not started yet.
 */
public class NuxeoDriveContribException extends ClientException {

    private static final long serialVersionUID = 1L;

    public NuxeoDriveContribException() {
    }

    public NuxeoDriveContribException(String message) {
        super(message);
    }

    public NuxeoDriveContribException(String message, ClientException cause) {
        super(message, cause);
    }

    public NuxeoDriveContribException(String message, Throwable cause) {
        super(message, cause);
    }

    public NuxeoDriveContribException(Throwable cause) {
        super(cause);
    }

    public NuxeoDriveContribException(ClientException cause) {
        super(cause);
    }

}
