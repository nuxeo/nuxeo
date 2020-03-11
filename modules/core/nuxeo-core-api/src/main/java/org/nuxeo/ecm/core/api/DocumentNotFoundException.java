/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * Exception thrown when a document is not found.
 */
public class DocumentNotFoundException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public DocumentNotFoundException() {
        super();
        this.statusCode = SC_NOT_FOUND;
    }

    public DocumentNotFoundException(String message) {
        super(message);
        this.statusCode = SC_NOT_FOUND;
    }

    public DocumentNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = SC_NOT_FOUND;
    }

    public DocumentNotFoundException(Throwable cause) {
        super(cause);
        this.statusCode = SC_NOT_FOUND;
    }

}
