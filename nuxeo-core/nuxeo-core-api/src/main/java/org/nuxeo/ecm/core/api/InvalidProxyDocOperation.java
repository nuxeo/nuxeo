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
package org.nuxeo.ecm.core.api;

/**
 * ClientException subclass specifying that a requested operation cannot be
 * performed onto a given document because of its proxy nature.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class InvalidProxyDocOperation extends ClientException {

    private static final long serialVersionUID = -4458350903944576757L;

    public InvalidProxyDocOperation() {
    }

    public InvalidProxyDocOperation(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidProxyDocOperation(String message) {
        super(message);
    }

    public InvalidProxyDocOperation(Throwable cause) {
        super(cause);
    }

}
