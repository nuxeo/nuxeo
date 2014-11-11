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
 *     dragos
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.impl;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Abstract RenderingContext subclass that adds capabilities of storing a
 * DocumentModel and retrieve RenderingConfig associated with the current set
 * DocumentModel.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public class DocumentRenderingContext extends DefaultRenderingContext {

    private static final long serialVersionUID = 1626664478541223492L;

    public static final String CTX_PARAM_DOCUMENT = "doc";

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(getClass())) {
            return adapter.cast(this);
        } else if (adapter.isAssignableFrom(DocumentModel.class)) {
            return adapter.cast(getDocument());
        }
        return null;
    }

    public void setDocument(DocumentModel doc) {
        put(CTX_PARAM_DOCUMENT, doc);
    }

    public DocumentModel getDocument() {
        return (DocumentModel) get(CTX_PARAM_DOCUMENT);
    }

}
