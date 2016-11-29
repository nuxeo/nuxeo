/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.exceptions;

import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebSecurityException extends WebException {

    private static final long serialVersionUID = 1L;

    protected final String action;

    public WebSecurityException(String message) {
        this(message, "");
    }

    public WebSecurityException(String message, String action) {
        super(message, Response.Status.FORBIDDEN.getStatusCode());
        this.action = action;
    }

    public WebSecurityException(String message, String action, Throwable cause) {
        super(message, cause, Response.Status.FORBIDDEN.getStatusCode());
        this.action = action;
    }

    public WebSecurityException(String message, Throwable cause) {
        this(message, "", cause);
    }

    public String getAction() {
        return action;
    }

}
