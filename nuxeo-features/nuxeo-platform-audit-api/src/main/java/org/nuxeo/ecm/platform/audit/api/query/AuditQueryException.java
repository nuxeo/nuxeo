/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: AuditQueryException.java 20577 2007-06-16 09:26:07Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api.query;

import org.nuxeo.ecm.platform.audit.api.AuditException;

/**
 * NXAudit-Query related exception.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class AuditQueryException extends AuditException {

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
