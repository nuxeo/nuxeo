/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api.model;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Exception indicating that a version cannot be modified.
 *
 * @since 10.3
 */
public class VersionNotModifiableException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public VersionNotModifiableException() {
        super();
    }

    public VersionNotModifiableException(String message) {
        super(message);
    }

    public VersionNotModifiableException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersionNotModifiableException(Throwable cause) {
        super(cause);
    }

}
