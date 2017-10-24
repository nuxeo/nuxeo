/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;

/**
 * An exception thrown if a concurrent update was detected.
 * <p>
 * Helpful for callers that may want to retry things.
 *
 * @since 5.8
 */
public class ConcurrentUpdateException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public ConcurrentUpdateException() {
        super(SC_CONFLICT);
    }

    public ConcurrentUpdateException(String message) {
        super(message, SC_CONFLICT);
    }

    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause, SC_CONFLICT);
    }

    public ConcurrentUpdateException(Throwable cause) {
        super(cause, SC_CONFLICT);
    }

}
