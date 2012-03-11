/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.publisher;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * 
 * Remove proxy to the same stored rendition with a different version.
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class RenditionsRemover extends UnrestrictedSessionRunner {

    protected final DocumentModel proxy;

    protected RenditionsRemover(DocumentModel source) {
        super(source.getCoreSession());
        this.proxy = source;
    }

    @Override
    public void run() throws ClientException {

        String targetUUID = (String) proxy.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY);

        String query = "select * from Document where ";
        query = query + RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY + "='"
                + targetUUID + "' ";

        query = query + " AND ecm:parentId='" + proxy.getParentRef().toString()
                + "'";

        List<DocumentModel> docs = session.query(query);
        for (DocumentModel doc : docs) {
            if (!doc.getId().equals(proxy.getId())) {
                session.removeDocument(doc.getRef());
            }
        }
    }

}
