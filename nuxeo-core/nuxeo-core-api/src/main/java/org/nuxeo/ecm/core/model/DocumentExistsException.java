/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Deprecated and never thrown, kept for compatibility so that old code catching this still works.
 * <p>
 * Use {@link org.nuxeo.ecm.core.api.DocumentExistsException} instead.
 *
 * @deprecated since 7.4, use org.nuxeo.ecm.core.api.DocumentExistsException instead
 */
@Deprecated
public class DocumentExistsException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public DocumentExistsException() {
    }

    public DocumentExistsException(String message) {
        super(message);
    }

    public DocumentExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentExistsException(Throwable cause) {
        super(cause);
    }

}
