/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.exception;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Distribution exception
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DamException extends ClientException {

    public DamException(String message) {
        super(message);
    }

    public DamException(String message, ClientException cause) {
        super(message, cause);
    }

    public DamException(String message, Throwable cause) {
        super(message, cause);
    }

    public DamException(Throwable cause) {
        super(cause);
    }

    public DamException(ClientException cause) {
        super(cause);
    }

}
