/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.ScopeType.STATELESS;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.jsf.JSFActionContext;
import org.nuxeo.ecm.platform.actions.seam.SeamActionContext;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;

@Name("actionContextProvider")
@Scope(STATELESS)
public class ActionContextProvider implements Serializable {

    private static final long serialVersionUID = 675765759871L;

    @In(create = true)
    private transient NavigationContext navigationContext;

    @In(create = true)
    private transient NuxeoPrincipal currentNuxeoPrincipal;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    // XXX AT: sometimes EVENT scope is not enough when changing the current
    // document several times in the same request. See
    // WebActionsBean#setCurrentTabAndNavigate hack for instance.
    @Factory(value = "currentActionContext", scope = EVENT)
    public ActionContext createActionContext() {
        return createActionContext(navigationContext.getCurrentDocument());
    }

    /**
     * Returns an action context computed from current core session and current user, and document given as parameter.
     * <p>
     * The action context uses the JSF context if available, or fallbacks on a Seam context only (useful for Seam
     * remoting calls, for instance in contextual menu)
     *
     * @since 5.7.3
     */
    public ActionContext createActionContext(DocumentModel document) {
        ActionContext ctx;
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            ctx = new SeamActionContext();
        } else {
            ctx = new JSFActionContext(faces);
        }
        ctx.setCurrentDocument(document);
        ctx.setDocumentManager(documentManager);
        ctx.setCurrentPrincipal(currentNuxeoPrincipal);
        ctx.putLocalVariable("SeamContext", new SeamContextHelper());
        return ctx;
    }

}
