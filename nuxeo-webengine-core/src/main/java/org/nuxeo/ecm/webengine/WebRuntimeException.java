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

package org.nuxeo.ecm.webengine;

import org.nuxeo.ecm.webengine.servlet.WebConst;

public class WebRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 176876876786L;

    private int returnCode = WebConst.SC_INTERNAL_SERVER_ERROR;

    public WebRuntimeException(String message) {
        super(message);
    }

    public WebRuntimeException(String message, int code) {
        super(message);
        returnCode = code;
    }

    public WebRuntimeException(String message, Throwable t) {
        super(message, t);
    }

    public WebRuntimeException(String message, Throwable t, int code) {
        super(message, t);
        returnCode = code;
    }

    public WebRuntimeException(WebException e) {
        this (e.getMessage(), e.getCause(), e.getReturnCode());
    }

    public int getReturnCode() {
        return returnCode;
    }

}
