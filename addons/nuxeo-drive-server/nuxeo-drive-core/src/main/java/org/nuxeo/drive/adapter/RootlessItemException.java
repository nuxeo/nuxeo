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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.drive.adapter;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Exception raised when recursive factory calls failed to find the ancestry to a the top level folder.
 */
public class RootlessItemException extends ClientException {

    private static final long serialVersionUID = 1L;

    public RootlessItemException() {
    }

    public RootlessItemException(String message) {
        super(message);
    }

    public RootlessItemException(String message, ClientException cause) {
        super(message, cause);
    }

    public RootlessItemException(String message, Throwable cause) {
        super(message, cause);
    }

    public RootlessItemException(Throwable cause) {
        super(cause);
    }

    public RootlessItemException(ClientException cause) {
        super(cause);
    }
}
