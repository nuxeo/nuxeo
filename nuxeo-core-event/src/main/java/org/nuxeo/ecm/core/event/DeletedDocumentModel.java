/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
