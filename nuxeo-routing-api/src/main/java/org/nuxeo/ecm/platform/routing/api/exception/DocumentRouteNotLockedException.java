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
 *     mcedica
 */
package org.nuxeo.ecm.platform.routing.api.exception;

/**
*
* @author <a href="mailto:mcedica@nuxeo.com">Mariana Cedica</a>
*
*/
public class DocumentRouteNotLockedException extends DocumentRouteException {

    private static final long serialVersionUID = 1L;

    public DocumentRouteNotLockedException() {
    }

    public DocumentRouteNotLockedException(String message) {
        super(message);
    }

    public DocumentRouteNotLockedException(Throwable th) {
        super(th);
    }

    public DocumentRouteNotLockedException(String message, Throwable th) {
        super(message, th);
    }

}
