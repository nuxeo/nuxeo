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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.heartbeat.api;

/**
 * Exception thrown when no server are found
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class ServerNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public ServerNotFoundException() {
        super();
    }

    public ServerNotFoundException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public ServerNotFoundException(String arg0) {
        super(arg0);
    }

    public ServerNotFoundException(Throwable arg0) {
        super(arg0);
    }

}
