/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Exception thrown by {@link FileSystemChangeFinder} when too many document changes are found in the audit logs.
 *
 * @author Antoine Taillefer
 */
public class TooManyChangesException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public TooManyChangesException(String message) {
        super(message);
    }

    public TooManyChangesException(String message, Throwable cause) {
        super(message, cause);
    }

    public TooManyChangesException(Throwable cause) {
        super(cause);
    }

}
