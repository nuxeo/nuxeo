package org.nuxeo.dam.platform.action;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.dam.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;
import org.nuxeo.ecm.webapp.action.WebActionsBean;
import org.nuxeo.ecm.webapp.security.UserManagerActions;
import org.nuxeo.runtime.api.Framework;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

/**
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
@Name("webActions")
@Scope(CONVERSATION)
@Install(precedence = Install.APPLICATION)
public class DamWebActionsBean extends WebActionsBean {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DamWebActionsBean.class);

    @In(create = true)
    private transient NuxeoPrincipal currentNuxeoPrincipal;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true, required = false)
    private transient DocumentActions documentActions;

    @In(create = true, required = false)
    private transient UserManagerActions userManagerActions;

    private ActionManager actionService;

    private boolean showList = false;

    private boolean showThumbnail = true;

    private boolean showAdministration = false;

    @Override
    @Factory(value = "tabsActionsList", scope = EVENT)
    public List<Action> getTabsList() {
        if (tabsActionsList == null) {
            actionService = getActionsService();
            tabsActionsList = actionService.getActions(
                    "VIEW_ASSET_ACTION_LIST", createActionContext());
            currentTabAction = getDefaultTab();
        }
        return tabsActionsList;
    }

    public ActionManager getActionsService() {
        if (actionService != null) {
            return actionService;
        }
        try {
            actionService = Framework.getService(ActionManager.class);
        } catch (Exception e) {
            log.error("Failed to lookup ActionService", e);
        }

        return actionService;
    }

    @Override
    protected ActionContext createActionContext() {
        ActionContext ctx = new ActionContext();
        ctx.setDocumentManager(documentManager);
        ctx.put("SeamContext", new SeamContextHelper());
        ctx.setCurrentPrincipal(currentNuxeoPrincipal);
        return ctx;
    }

    public String navigateToAdministration() throws ClientException {
        showAdministration = true;
        return userManagerActions.viewUsers();
    }

    public String navigateToAssetManagement() throws ClientException {
        showAdministration = false;
        return navigationContext.goHome();
    }

    @Factory(value = "isInsideAdministration", scope = ScopeType.EVENT)
    public boolean showAdministration() {
        return showAdministration;
    }

    public void showListLink() {
        if (showList) {
            return;
        }
        this.showList = !this.showList;
        this.showThumbnail = !this.showThumbnail;
    }

    public void showThumbnailLink() {
        if (showThumbnail) {
            return;
        }
        this.showThumbnail = !this.showThumbnail;
        this.showList = !this.showList;
    }

    public boolean getShowList() {
        return showList;
    }

    public void setShowList(boolean showList) {
        this.showList = showList;
    }

    public boolean getShowThumbnail() {
        return showThumbnail;
    }

    public void setShowThumbnail(boolean showThumbnail) {
        this.showThumbnail = showThumbnail;
    }

}
