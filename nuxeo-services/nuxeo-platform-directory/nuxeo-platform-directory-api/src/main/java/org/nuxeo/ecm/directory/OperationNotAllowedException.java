/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.RecoverableClientException;

/**
 * Exception thrown when an operation is not allowed on a given directory entry.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class OperationNotAllowedException extends RecoverableClientException {

    private static final long serialVersionUID = -1L;

    public OperationNotAllowedException(String message, String localizedMessage, String[] params) {
        super(message, localizedMessage, params);
    }

    public OperationNotAllowedException(String message, String localizedMessage, String[] params, Throwable cause) {
        super(message, localizedMessage, params, cause);
    }

}
