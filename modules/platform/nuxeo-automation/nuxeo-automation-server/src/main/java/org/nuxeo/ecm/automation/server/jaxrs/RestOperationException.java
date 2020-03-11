/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
