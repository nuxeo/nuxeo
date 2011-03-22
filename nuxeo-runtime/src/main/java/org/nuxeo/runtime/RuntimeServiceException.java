/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

/**
 * Describes a runtime service exception.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RuntimeServiceException extends Exception {

    private static final long serialVersionUID = -8282398803917588207L;

    public RuntimeServiceException() {
    }

    public RuntimeServiceException(String message) {
        super(message);
    }

    public RuntimeServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeServiceException(Throwable cause) {
        super(cause);
    }

}
