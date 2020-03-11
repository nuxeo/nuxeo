/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory;

/**
 * Exception thrown when access to a directory is denied.
 * <p>
 * Access can be denied for Read/Write when the directory is configured with permissions or is read-only and the current
 * user does not have adequate privileges for the attempted method.
 *
 * @since 8.3
 */
public class DirectorySecurityException extends DirectoryException {

    private static final long serialVersionUID = 1L;

    public DirectorySecurityException() {
    }

    public DirectorySecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public DirectorySecurityException(String message) {
        super(message);
    }

    public DirectorySecurityException(Throwable cause) {
        super(cause);
    }

}
