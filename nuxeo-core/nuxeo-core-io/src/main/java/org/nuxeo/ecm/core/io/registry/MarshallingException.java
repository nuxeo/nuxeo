/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * {@link ClientException} thrown by the {@link MarshallerRegistry} and all {@link Marshaller}s.
 *
 * @since 7.2
 */
public class MarshallingException extends ClientException {

    private static final long serialVersionUID = 1L;

    public MarshallingException() {
        super();
    }

    public MarshallingException(ClientException cause) {
        super(cause);
    }

    public MarshallingException(String message, ClientException cause) {
        super(message, cause);
    }

    public MarshallingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MarshallingException(String message) {
        super(message);
    }

    public MarshallingException(Throwable cause) {
        super(cause);
    }

}
