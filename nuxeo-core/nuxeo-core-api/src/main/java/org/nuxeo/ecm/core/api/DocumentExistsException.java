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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

/**
 * Exception thrown when a method tries to create a document that already exists through copy or move, or when trying to
 * delete the target of a proxy.
 *
 * @see CoreSession#copy
 * @see CoreSession#move
 * @see CoreSession#removeDocument
 * @see CoreSession#removeDocuments
 * @see CoreSession#removeChildren
 */
// inherits from a deprecated base DocumentExistsException so that old code catching the old one still works
@SuppressWarnings("deprecation")
public class DocumentExistsException extends org.nuxeo.ecm.core.model.DocumentExistsException {

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
