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
package org.nuxeo.ecm.automation.server.jaxrs;

import org.nuxeo.ecm.automation.OperationException;

import javax.servlet.http.HttpServletResponse;

/**
 * Automation exception to extend to be thrown during REST calls on
 * Automation operations.
 *
 * @since 7.1
 */
public class RestOperationException extends OperationException {

    private static final long serialVersionUID = 1L;

    protected int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    public RestOperationException(String message) {
        super(message);
    }

    public RestOperationException(Throwable cause) {
        super(cause);
    }

    public RestOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestOperationException(String message, int status) {
        super(message);
        this.status = status;
    }

    public RestOperationException(Throwable cause, int status) {
        super(cause);
        this.status = status;
    }

    public RestOperationException(String message, Throwable cause, int status) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
