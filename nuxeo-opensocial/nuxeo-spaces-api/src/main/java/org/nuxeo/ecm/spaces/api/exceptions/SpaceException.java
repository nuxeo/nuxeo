/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.api.exceptions;

import org.nuxeo.ecm.core.api.ClientException;

public class SpaceException extends ClientException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public SpaceException() {
        super();
    }

    public SpaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpaceException(String message) {
        super(message);
    }

    public SpaceException(Throwable cause) {
        super(cause);
    }

}
