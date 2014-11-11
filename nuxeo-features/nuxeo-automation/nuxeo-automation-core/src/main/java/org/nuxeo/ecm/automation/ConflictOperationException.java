/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.nuxeo.ecm.automation;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Marker Exception to identify Dirty update detected by using the ChangeToken
 * of the DocumentModel
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class ConflictOperationException extends OperationException {

    private static final long serialVersionUID = 1L;

    public ConflictOperationException(DocumentModel doc) {
        super("Conflict detected while trying to update document "
                + doc.getId());
    }
}
