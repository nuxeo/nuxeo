/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Wojciech Sulejman
 */

package org.nuxeo.ecm.platform.signature.api.exception;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * A generic exception indicating signature generation problems
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public class SignException extends ClientException {

    private static final long serialVersionUID = 532586426137845735L;

    public SignException(String message) {
        super(message);
    }

    public SignException(String message, ClientException cause) {
        super(message, cause);
    }

    public SignException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignException(Throwable cause) {
        super(cause);
    }

    public SignException(ClientException cause) {
        super(cause);
    }

}
