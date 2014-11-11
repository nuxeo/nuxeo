/*******************************************************************************
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *******************************************************************************/
package org.nuxeo.ecm.core.api.local;

import org.nuxeo.ecm.core.api.ClientRuntimeException;

public class LocalException extends ClientRuntimeException {

    public LocalException(String message, Throwable t) {
        super(message, t);
    }

    public LocalException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 1L;

}