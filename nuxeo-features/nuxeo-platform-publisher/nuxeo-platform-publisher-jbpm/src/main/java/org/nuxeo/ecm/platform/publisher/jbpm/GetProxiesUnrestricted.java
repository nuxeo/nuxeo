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
 *     arussel
 */
package org.nuxeo.ecm.platform.publisher.jbpm;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * @author arussel
 *
 */
public class GetProxiesUnrestricted extends UnrestrictedSessionRunner {
    protected DocumentModelList result;
    protected DocumentModel folder;
    protected DocumentModel doc;

    public GetProxiesUnrestricted(CoreSession session, DocumentModel folder,
            DocumentModel doc) {
        super(session);
        this.folder = folder;
        this.doc = doc;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.core.api.UnrestrictedSessionRunner#run()
     */
    @Override
    public void run() throws ClientException {
        result = session.getProxies(doc.getRef(), folder.getRef());
    }

    public DocumentModelList getDocumentModelList() {
        return result;
    }
}
