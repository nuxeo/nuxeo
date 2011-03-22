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

package org.nuxeo.runtime.api;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BindingException extends Exception {

    private static final long serialVersionUID = 7176949285202523625L;

    public BindingException(String message) {
        super(message);
    }

    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }

}
