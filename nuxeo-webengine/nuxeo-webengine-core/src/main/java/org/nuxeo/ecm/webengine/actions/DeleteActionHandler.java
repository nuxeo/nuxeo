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

package org.nuxeo.ecm.webengine.actions;


import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DeleteActionHandler implements ActionHandler {

    public void run(WebObject object) throws WebException {
        DocumentModel doc = object.getDocument();
        if (doc != null) {
            try {
                DocumentRef parentRef = doc.getParentRef();
                WebContext ctx = object.getWebContext();
                CoreSession session = ctx.getCoreSession();
                session.removeDocument(doc.getRef());
                session.save();
                doc = session.getDocument(parentRef);
                String url = ctx.getUrlPath(doc);
                ctx.redirect(url);
            } catch (Exception e) {
                throw new WebException("Failed to delete document", e);
            }
        }
    }

}
