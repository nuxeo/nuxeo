/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ActionContext.java 20218 2007-06-07 19:19:46Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.util.HashMap;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 *
 */
// TODO: this is
//         imposing limitation on action service to run on same machine as the
//         web client because it is using the documentmanager instance from the
//         web client
public class ActionContext extends HashMap<String, Object> {

    private static final long serialVersionUID = -8286890979128279598L;

    private DocumentModel currentDocument;

    private CoreSession docMgr;

    private NuxeoPrincipal currentPrincipal;

    public final void setCurrentDocument(DocumentModel doc) {
        currentDocument = doc;
    }

    public final DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    public final CoreSession getDocumentManager() {
        return docMgr;
    }

    public final void setDocumentManager(CoreSession docMgr) {
        this.docMgr = docMgr;
    }

    public final NuxeoPrincipal getCurrentPrincipal() {
        return currentPrincipal;
    }

    public final void setCurrentPrincipal(NuxeoPrincipal currentPrincipal) {
        this.currentPrincipal = currentPrincipal;
    }

}
