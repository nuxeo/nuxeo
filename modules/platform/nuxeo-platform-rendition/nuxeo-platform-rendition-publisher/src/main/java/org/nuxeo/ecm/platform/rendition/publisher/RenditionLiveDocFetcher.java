/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Contributors:
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.publisher;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * Fetched the live doc for a given proxy on a Rendition
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
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
    public void run() {

        String targetUUID = (String) proxy.getPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY);
        liveDocument = session.getDocument(new IdRef(targetUUID));
        liveDocument.detach(true);
        liveDocument.attach(sid);
    }

    public DocumentModel getLiveDocument() {
        return liveDocument;
    }

}
