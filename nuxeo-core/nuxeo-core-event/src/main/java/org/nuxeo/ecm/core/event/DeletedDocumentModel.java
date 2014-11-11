/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     eugen
 */
package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 *
 */
public class DeletedDocumentModel extends ShallowDocumentModel{

    private static final long serialVersionUID = 1L;

    public DeletedDocumentModel(DocumentModel doc) {
        super(doc);
    }

}
