package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.STATELESS;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.ActionContext;
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

    @Factory(value = "currentActionContext", scope = EVENT)
    public ActionContext createActionContext() {
        ActionContext ctx = new ActionContext();
        ctx.setCurrentDocument(navigationContext.getCurrentDocument());
        ctx.setDocumentManager(documentManager);
        ctx.put("SeamContext", new SeamContextHelper());
        ctx.setCurrentPrincipal(currentNuxeoPrincipal);
        return ctx;
    }
}
