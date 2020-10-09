/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Computes the URL of a document.
 * <p>
 * It expects the document to be in the context under the key "document" and the baseUrl at baseUrl. It puts the URL at
 * key "url".
 *
 * @author Alexandre Russel
 */
public class DocumentURLAction implements MessageAction {

    private static final Log log = LogFactory.getLog(DocumentURLAction.class);

    protected DocumentViewCodecManager documentViewCodecManager;

    protected final String baseUrl;

    public DocumentURLAction(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean execute(ExecutionContext context) {
        DocumentModel documentModel = (DocumentModel) context.get("document");
        if (log.isDebugEnabled()) {
            log.debug("Document url computing for doc: " + documentModel);
        }
        documentViewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        DocumentView docView = new DocumentViewImpl(documentModel);
        String url = documentViewCodecManager.getUrlFromDocumentView(docView, true, baseUrl);
        context.put("url", url);
        return true;
    }

    @Override
    public void reset(ExecutionContext context) {
        // do nothing
    }

}
