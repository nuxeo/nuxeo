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
package org.nuxeo.ecm.automation.test.helpers;

import org.nuxeo.ecm.automation.server.jaxrs.RestOperationException;

public class ExceptionTest extends RestOperationException {

    private static final long serialVersionUID = 7123858603327032114L;

    public ExceptionTest(String message, Throwable cause) {
        super(message, cause);
    }

    public ExceptionTest(String message) {
        super(message);
    }

    public ExceptionTest(Throwable cause) {
        super(cause);
    }
}
