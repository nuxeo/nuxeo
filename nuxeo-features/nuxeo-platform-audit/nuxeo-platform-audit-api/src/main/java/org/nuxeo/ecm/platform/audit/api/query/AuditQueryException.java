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
 * $Id: AuditQueryException.java 20577 2007-06-16 09:26:07Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api.query;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * NXAudit-Query related exception.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class AuditQueryException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public AuditQueryException() {
    }

    public AuditQueryException(String message) {
        super(message);
    }

    public AuditQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuditQueryException(Throwable cause) {
        super(cause);
    }

}
