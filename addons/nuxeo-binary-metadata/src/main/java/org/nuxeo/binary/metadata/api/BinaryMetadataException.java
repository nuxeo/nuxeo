/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.api;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * @since 7.1
 */
public class BinaryMetadataException extends ClientException {

    private static final long serialVersionUID = 1L;

    public BinaryMetadataException() {
    }

    public BinaryMetadataException(String message) {
        super(message);
    }

    public BinaryMetadataException(String message, ClientException cause) {
        super(message, cause);
    }

    public BinaryMetadataException(String message, Throwable cause) {
        super(message, cause);
    }

    public BinaryMetadataException(Throwable cause) {
        super(cause);
    }

    public BinaryMetadataException(ClientException cause) {
        super(cause);
    }

}
