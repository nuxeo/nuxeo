/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.platform.context;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.dam.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * Helper for managing context.
 *
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 */
@Name("urlPatternHelper")
@Scope(EVENT)
public class URLPatternHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient DocumentActions documentActions;

    private DocumentView docView;

    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String initContextFromRestRequest(DocumentView docView)
            throws ClientException {
        return null;
    }

    public DocumentView getDocumentView() {
        return docView;
    }

    public void setDocumentView(DocumentView docView) {
        this.docView = docView;
    }

    public DocumentView getNewDocumentView() throws ClientException {
        DocumentView docView = null;
        DocumentModel currentDocument = documentActions.getCurrentSelection();
        if (currentDocument != null) {
            DocumentLocation docLoc = new DocumentLocationImpl(currentDocument);
            docView = new DocumentViewImpl(docLoc);
        }
        return docView;
    }

}
