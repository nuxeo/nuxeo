package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.webapp.helpers.EventNames.NAVIGATE_TO_DOCUMENT;

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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
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
 * @since 5.4.2
 */
@Name("mainTabsActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class MainTabsActions implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * @deprecated since 5.5, use {@link WebActions#MAIN_TABS_CATEGORY}
     *             instead
     */
    @Deprecated
    public static final String MAIN_TABS_CATEGORY = WebActions.MAIN_TABS_CATEGORY;

    /**
     * @deprecated since 5.5, use {@link WebActions#DOCUMENTS_MAIN_TAB_ID}
     *             instead
     */
    @Deprecated
    public static final String DOCUMENT_MANAGEMENT_ACTION = WebActions.DOCUMENTS_MAIN_TAB_ID;

    /**
     * @deprecated since 5.5, use {@link WebActions#TAB_IDS_PARAMETER}
     *             instead
     */
    @Deprecated
    public static final String TAB_IDS_PARAMETER = WebActions.TAB_IDS_PARAMETER;

    /**
     * @deprecated since 5.5, use {@link WebActions#MAIN_TAB_ID_PARAMETER}
     *             instead
     */
    @Deprecated
    public static final String MAIN_TAB_ID_PARAMETER = WebActions.MAIN_TAB_ID_PARAMETER;

    public static final String DEFAULT_VIEW = "view_documents";

    @In(create = true)
    protected transient RepositoryManager repositoryManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true, required = false)
    protected transient ActionManager actionManager;

    protected Map<String, DocumentModel> documentsByMainTabs = new HashMap<String, DocumentModel>();

    @Observer({ NAVIGATE_TO_DOCUMENT })
    public void updateContextualDocument() {
        if (!shouldHandleRequest()) {
            return;
        }
        String currentMainTab = getCurrentMainTabFromRequest();
        if (currentMainTab == null) {
            currentMainTab = webActions.getCurrentTabId(WebActions.MAIN_TABS_CATEGORY);
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        documentsByMainTabs.put(currentMainTab, currentDocument);
    }

    /**
     * Only handle non POST requests
     */
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
                String tabIds = docView.getParameter(WebActions.TAB_IDS_PARAMETER);
                String mainTabId = docView.getParameter(WebActions.MAIN_TAB_ID_PARAMETER);
                if (mainTabId != null && !mainTabId.isEmpty()) {
                    tabIds = mainTabId;
                }
                if (tabIds != null
                        && tabIds.contains(WebActions.MAIN_TABS_CATEGORY)) {
                    String[] encodedActions = tabIds.split(",");
                    for (String encodedAction : encodedActions) {
                        if (encodedAction.startsWith(WebActions.MAIN_TABS_CATEGORY)) {
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

    /**
     * Set the document used for a given {@code mainTabId}.
     *
     * @since 5.7
     */
    public void setDocumentFor(String mainTabId, DocumentModel doc) {
        documentsByMainTabs.put(mainTabId, doc);
    }

    public DocumentModel getDocumentFor(String mainTabId)
            throws ClientException {
        return getDocumentFor(mainTabId, navigationContext.getCurrentDocument());
    }

    public DocumentModel getDocumentFor(String mainTabId,
            DocumentModel defaultDocument) throws ClientException {
        DocumentModel doc = documentsByMainTabs.get(mainTabId);
        if (doc == null
                || !documentManager.exists(doc.getRef())
                || !documentManager.hasPermission(doc.getRef(),
                        SecurityConstants.READ)) {
            documentsByMainTabs.put(mainTabId, defaultDocument);
            doc = null;
        }
        return doc != null ? doc : defaultDocument;
    }

    public String getViewFor(Action mainTabAction) throws ClientException {
        if (!mainTabAction.getId().equals(WebActions.DOCUMENTS_MAIN_TAB_ID)) {
            return mainTabAction.getLink();
        }

        DocumentModel doc = getDocumentFor(mainTabAction.getId(),
                navigationContext.getCurrentDocument());
        if (doc != null) {
            TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
            return typeInfo.getDefaultView();
        }
        return DEFAULT_VIEW;
    }

    public String getViewFor(String mainTabId) throws ClientException {
        Action mainTabAction = actionManager.getAction(mainTabId);
        return mainTabAction != null ? getViewFor(mainTabAction) : null;
    }

    public String getPatternFor(String mainTabId) throws ClientException {
        try {
            URLPolicyService service = Framework.getService(URLPolicyService.class);
            // FIXME: find some way to reference the pattern in the action,
            // assume the pattern will be the same than the default one for
            // now, or use the default one.
            if (!WebActions.DOCUMENTS_MAIN_TAB_ID.equals(mainTabId)
                    && service.hasPattern(mainTabId)) {
                return mainTabId;
            }
            return service.getDefaultPatternName();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public boolean isOnMainTab(String mainTabId) {
        if (mainTabId != null
                && mainTabId.equals(webActions.getCurrentTabId(WebActions.MAIN_TABS_CATEGORY))) {
            return true;
        }
        return false;
    }

}
