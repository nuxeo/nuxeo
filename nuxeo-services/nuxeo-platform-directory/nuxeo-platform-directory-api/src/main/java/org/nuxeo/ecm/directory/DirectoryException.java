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
 *     George Lefter
 */
package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Exception thrown when dealing with a Directory.
 */
public class DirectoryException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public DirectoryException() {
    }

    public DirectoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public DirectoryException(String message) {
        super(message);
    }

    public DirectoryException(Throwable cause) {
        super(cause);
    }

    /**
     * @since 11.1
     */
    public DirectoryException(int statusCode) {
        super(statusCode);
    }

    /**
     * @since 11.1
     */
    public DirectoryException(String message, int statusCode) {
        super(message, statusCode);
    }

    /**
     * @since 11.1
     */
    public DirectoryException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }

    /**
     * @since 11.1
     */
    public DirectoryException(Throwable cause, int statusCode) {
        super(cause, statusCode);
    }

}
