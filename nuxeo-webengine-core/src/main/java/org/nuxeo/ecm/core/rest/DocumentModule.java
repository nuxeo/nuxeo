/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.impl.DefaultModule;

/**
 * Base class for modules that needs to bind to a document model
 * TODO: this class is not working yet.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentModule extends DefaultModule {

    protected DocumentModel doc;

    public DocumentModule(String path) {
        this (new PathRef(path));
    }

    public DocumentModule(DocumentRef ref) {
        try {
            doc = ctx.getCoreSession().getDocument(ref);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public CoreSession getSession() {
        return ctx.getCoreSession();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A getAdapter(Class<A> adapter) {
        if (DocumentModel.class.isAssignableFrom(adapter)) {
            return (A)doc;
        }
        return super.getAdapter(adapter);
    }

}
