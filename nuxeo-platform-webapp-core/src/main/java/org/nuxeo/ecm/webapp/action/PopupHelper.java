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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.PAGE;

import java.util.ArrayList;
import java.util.List;


import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.contexts.Context;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

//@Name("popupHelper")
//@Scope(SESSION)
@SerializedConcurrentAccess
public class PopupHelper {

    @In(required = false)
    @Out(required = false, scope = PAGE)
    protected String popupDocId;

    @In(required = false)
    protected DocumentModel currentDocument;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In
    protected transient Context sessionContext;

    @In(create = true)
    protected WebActions webActions;


    public String getPopupDocId() {
        if (popupDocId == null) {
            return "";
        }
        return popupDocId;
    }

    public void setPopupDocRef(DocumentRef popupDocId) {
        this.popupDocId = popupDocId.toString();
    }

    public void setPopupDocId(String popupDocId) {
        this.popupDocId = popupDocId;
    }

    public List<Action> getPopupActions() {
        if (popupDocId == null || "".equals(popupDocId)) {
            return new ArrayList<Action>();
        }
        List<Action> actions = webActions.getActionsList("POPUP",
                createActionContext());

        // post filters links to add docId
        for (Action act : actions) {
            String lnk = act.getLink();
            lnk = lnk.replaceFirst("popupDoc", '\'' + popupDocId + "'");
            act.setLink(lnk);
        }
        return actions;
    }

    protected ActionContext createActionContext() {
        ActionContext ctx = new ActionContext();
        ctx.setCurrentDocument(currentDocument);
        ctx.setDocumentManager(documentManager);

        ctx.put("SeamContext", sessionContext);
        ctx.setCurrentPrincipal((NuxeoPrincipal) documentManager.getPrincipal());

        DocumentRef popupDocRef = new IdRef(popupDocId);
        try {
            DocumentModel popupDoc = documentManager.getDocument(popupDocRef);
            ctx.put("popupDoc", popupDoc);
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ctx;
    }

}
