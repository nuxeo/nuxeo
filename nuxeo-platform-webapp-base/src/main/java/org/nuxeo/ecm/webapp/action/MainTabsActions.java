package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.webapp.helpers.EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.api.Framework;

/**
 * Handle Main tab related actions.
 * <p>
 * Maintains a Map of tab id -> contextual document.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("mainTabsActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class MainTabsActions implements Serializable {

    public static final String MAIN_TABS_CATEGORY = "MAIN_TABS";

    public static final String TAB_IDS_PARAMETER = "tabIds";

    public static final String MAIN_TAB_ID_PARAMETER = "mainTabId";

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    protected Map<String, DocumentModel> documentsByMainTabs = new HashMap<String, DocumentModel>();

    @Observer({ USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED })
    public void updateContextualDocument() {
        if (!shouldHandleRequest()) {
            return;
        }
        String currentMainTab = getCurrentMainTabFromRequest();
        if (currentMainTab == null) {
            currentMainTab = webActions.getCurrentTabId(MAIN_TABS_CATEGORY);
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        documentsByMainTabs.put(currentMainTab, currentDocument);
    }

    protected boolean shouldHandleRequest() {
        ServletRequest request = (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            return !httpServletRequest.getMethod().equalsIgnoreCase("post");
        }
        return false;
    }

    protected String getCurrentMainTabFromRequest() {
        try {
            URLPolicyService service = Framework.getService(URLPolicyService.class);
            ServletRequest request = (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            if (request instanceof HttpServletRequest) {
                DocumentView docView = service.getDocumentViewFromRequest((HttpServletRequest) request);
                if (docView == null) {
                    return null;
                }
                String tabIds = docView.getParameter(TAB_IDS_PARAMETER);
                String mainTabId = docView.getParameter(MAIN_TAB_ID_PARAMETER);
                if (mainTabId != null && !mainTabId.isEmpty()) {
                    tabIds = mainTabId;
                }
                if (tabIds != null && tabIds.contains(MAIN_TABS_CATEGORY)) {
                    String[] encodedActions = tabIds.split(",");
                    for (String encodedAction : encodedActions) {
                        if (encodedAction.startsWith(MAIN_TABS_CATEGORY)) {
                            String[] actionInfo = encodedAction.split(":");
                            if (actionInfo != null && actionInfo.length > 1) {
                                return actionInfo[1];
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    public DocumentModel getDocumentFor(String mainTabId) {
        return getDocumentFor(mainTabId, navigationContext.getCurrentDocument());
    }

    public DocumentModel getDocumentFor(String mainTabId,
            DocumentModel defaultDocument) {
        DocumentModel doc = documentsByMainTabs.get(mainTabId);
        return doc != null ? doc : defaultDocument;
    }

}
