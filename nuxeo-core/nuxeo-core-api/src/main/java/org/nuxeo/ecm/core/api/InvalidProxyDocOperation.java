/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.api;

/**
 * ClientException subclass specifying that a requested operation cannot be
 * performed onto a given document because of its proxy nature.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class InvalidProxyDocOperation extends ClientException {

    private static final long serialVersionUID = -4458350903944576757L;

    public InvalidProxyDocOperation() {
    }

    public InvalidProxyDocOperation(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidProxyDocOperation(String message) {
        super(message);
    }

    public InvalidProxyDocOperation(Throwable cause) {
        super(cause);
    }

}
