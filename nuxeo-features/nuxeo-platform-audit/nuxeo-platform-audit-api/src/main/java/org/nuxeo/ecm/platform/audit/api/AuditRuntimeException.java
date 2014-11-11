/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.ecm.platform.audit.api;

import org.nuxeo.ecm.core.api.ClientRuntimeException;

public class AuditRuntimeException extends ClientRuntimeException {

    private static final long serialVersionUID = -7230317313024997816L;

    public AuditRuntimeException(String message, Throwable t) {
        super(message, t);
    }

    public AuditRuntimeException(String message) {
        super(message);
    }

}
