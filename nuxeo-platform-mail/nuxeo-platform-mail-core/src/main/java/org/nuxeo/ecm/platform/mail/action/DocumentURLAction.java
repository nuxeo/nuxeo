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
 * It expects the document to be in the context under the key "document"
 *  and the baseUrl at baseUrl. It puts the URL at key "url".
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

    public boolean execute(ExecutionContext context) throws Exception {
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

    public void reset(ExecutionContext context) throws Exception {
        //do nothing
    }

}
