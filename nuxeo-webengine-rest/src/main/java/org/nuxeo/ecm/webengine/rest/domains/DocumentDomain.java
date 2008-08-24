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

package org.nuxeo.ecm.webengine.rest.domains;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.adapters.DocumentObject;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentDomain extends WebDomain<DomainDescriptor> {

    protected String repository = "default"; // TODO

    public DocumentDomain(WebEngine2 engine, DomainDescriptor desc) throws WebException {
        super (engine, desc );
    }

    @Override
    protected WebObject resolve(WebContext2 ctx, String path) throws WebException {
        try {
            DocumentModel doc = ctx.getCoreSession().getDocument(new PathRef(descriptor.root+"/"+path));
            DocumentObject obj = (DocumentObject)ctx.getEngine().getWebTypeManager().newInstance(doc.getType());
            obj.initialize(ctx, doc);
            return obj;
        } catch (Exception e) {
            throw WebException.wrap("Failed to resolve request", e);
        }
    }

}
