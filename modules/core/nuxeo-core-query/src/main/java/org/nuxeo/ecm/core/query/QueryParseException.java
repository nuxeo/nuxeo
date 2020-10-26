/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class QueryParseException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public QueryParseException() {
        super(SC_BAD_REQUEST);
    }

    public QueryParseException(String message) {
        super(message, SC_BAD_REQUEST);
    }

    public QueryParseException(String message, Throwable cause) {
        super(message, cause, SC_BAD_REQUEST);
    }

    public QueryParseException(Throwable cause) {
        super(cause, SC_BAD_REQUEST);
    }

}
