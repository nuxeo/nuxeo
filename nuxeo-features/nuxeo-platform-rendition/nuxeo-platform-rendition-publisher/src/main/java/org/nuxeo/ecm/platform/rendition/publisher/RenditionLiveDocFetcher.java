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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * 
 * Fetched the live doc for a given proxy on a Rendition
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class RenditionLiveDocFetcher extends UnrestrictedSessionRunner {

    protected final DocumentModel proxy;

    protected final String sid;

    protected DocumentModel liveDocument;

    protected RenditionLiveDocFetcher(CoreSession session, DocumentModel source) {
        super(session);
        this.proxy = source;
        this.sid = session.getSessionId();
    }

    @Override
    public void run() throws ClientException {

        String targetUUID = (String) proxy.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY);
        liveDocument = session.getDocument(new IdRef(targetUUID));
        liveDocument.detach(true);
        liveDocument.attach(sid);
    }

    public DocumentModel getLiveDocument() {
        return liveDocument;
    }

}
