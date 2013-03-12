/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.dam.seam;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.platform.contentview.jsf.ContentView.CONTENT_VIEW_PAGE_CHANGED_EVENT;
import static org.nuxeo.ecm.platform.contentview.jsf.ContentView.CONTENT_VIEW_PAGE_SIZE_CHANGED_EVENT;
import static org.nuxeo.ecm.platform.contentview.jsf.ContentView.CONTENT_VIEW_REFRESH_EVENT;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.json.JSONContentViewState;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7
 */
@Name("damSearchActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DamSearchActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DamSearchActions.class);

    public static final String DAM_FLAG = "DAM";

    public static final String DAM_CODEC = "docpathdam";

    public static final String CONTENT_VIEW_NAME_PARAMETER = "contentViewName";

    public static final String CURRENT_PAGE_PARAMETER = "currentPage";

    public static final String PAGE_SIZE_PARAMETER = "pageSize";

    public static final String CONTENT_VIEW_STATE_PARAMETER = "state";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected RestHelper restHelper;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    protected List<String> contentViewNames;

    protected Set<ContentViewHeader> contentViewHeaders;

    protected String currentContentViewName;

    protected String currentPage;

    protected String pageSize;

    public String getCurrentContentViewName() {
        if (currentContentViewName == null) {
            List<String> contentViewNames = getContentViewNames();
            if (!contentViewNames.isEmpty()) {
                currentContentViewName = contentViewNames.get(0);
            }
        }
        return currentContentViewName;
    }

    public void setCurrentContentViewName(String currentContentViewName) {
        this.currentContentViewName = currentContentViewName;
    }

    public List<String> getContentViewNames() {
        if (contentViewNames == null) {
            ContentViewService contentViewService = Framework.getLocalService(ContentViewService.class);
            contentViewNames = new ArrayList<String>(
                    contentViewService.getContentViewNames(DAM_FLAG));
        }
        return contentViewNames;
    }

    public Set<ContentViewHeader> getContentViewHeaders()
            throws ClientException {
        if (contentViewHeaders == null) {
            contentViewHeaders = new HashSet<ContentViewHeader>();
            ContentViewService contentViewService = Framework.getLocalService(ContentViewService.class);
            for (String name : getContentViewNames()) {
                ContentViewHeader header = contentViewService.getContentViewHeader(name);
                if (header != null) {
                    contentViewHeaders.add(header);
                }
            }
        }
        return contentViewHeaders;
    }

    public void clearSearch() throws ClientException {
        contentViewActions.reset(getCurrentContentViewName());
        updateCurrentDocument();
    }

    public void refreshAndRewind() throws ClientException {
        contentViewActions.refreshAndRewind(getCurrentContentViewName());
        updateCurrentDocument();
    }

    @SuppressWarnings("unchecked")
    public void updateCurrentDocument() throws ClientException {
        ContentView contentView = contentViewActions.getContentView(getCurrentContentViewName());
        updateCurrentDocument((PageProvider<DocumentModel>) contentView.getCurrentPageProvider());
    }

    public void updateCurrentDocument(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        if (pageProvider == null) {
            return;
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        List<DocumentModel> docs = pageProvider.getCurrentPage();
        if (docs.isEmpty()) {
            // no document selected
            navigationContext.setCurrentDocument(null);
        } else if (!docs.contains(currentDocument)) {
            navigationContext.setCurrentDocument(docs.get(0));
        }
    }

    /*
     * ----- AJAX page navigation -----
     */

    public void firstPage(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        pageProvider.firstPage();
        updateCurrentDocument(pageProvider);
    }

    public void previousPage(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        pageProvider.previousPage();
        updateCurrentDocument(pageProvider);
    }

    public void nextPage(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        pageProvider.nextPage();
        updateCurrentDocument(pageProvider);
    }

    public void lastPage(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        pageProvider.lastPage();
        updateCurrentDocument(pageProvider);
    }

    public void setState(String state) throws ClientException,
            UnsupportedEncodingException {
        if (StringUtils.isNotBlank(state)) {
            Long finalPageSize = null;
            if (!StringUtils.isBlank(pageSize)) {
                try {
                    finalPageSize = Long.valueOf(pageSize);
                } catch (NumberFormatException e) {
                    log.warn(String.format(
                            "Unable to parse '%s' parameter with value '%s'",
                            PAGE_SIZE_PARAMETER, pageSize));
                }
            }

            Long finalCurrentPage = null;
            if (!StringUtils.isBlank(currentPage)) {
                try {
                    finalCurrentPage = Long.valueOf(currentPage);
                } catch (NumberFormatException e) {
                    log.warn(String.format(
                            "Unable to parse '%s' parameter with value '%s'",
                            CURRENT_PAGE_PARAMETER, currentPage));
                }
            }

            contentViewActions.restoreContentView(getCurrentContentViewName(),
                    finalCurrentPage, finalPageSize, null, state);
        }
        updateCurrentDocument();
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Compute a permanent link for the current search.
     */
    @SuppressWarnings("unchecked")
    public String getPermanentLinkUrl() throws ClientException,
            UnsupportedEncodingException {
        String currentContentViewName = getCurrentContentViewName();
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentView docView = computeDocumentView(currentDocument);
        docView.setViewId("assets");
        docView.addParameter(CONTENT_VIEW_NAME_PARAMETER,
                currentContentViewName);
        ContentView contentView = contentViewActions.getContentView(currentContentViewName);
        ContentViewService contentViewService = Framework.getLocalService(ContentViewService.class);
        ContentViewState state = contentViewService.saveContentView(contentView);
        docView.addParameter(CONTENT_VIEW_STATE_PARAMETER,
                JSONContentViewState.toJSON(state, true));
        DocumentViewCodecManager documentViewCodecManager = Framework.getLocalService(DocumentViewCodecManager.class);
        String url = documentViewCodecManager.getUrlFromDocumentView(DAM_CODEC,
                docView, true, BaseURL.getBaseURL());
        return RestHelper.addCurrentConversationParameters(url);
    }

    protected DocumentView computeDocumentView(DocumentModel doc) {
        if (doc != null) {
            return new DocumentViewImpl(new DocumentLocationImpl(
                    documentManager.getRepositoryName(), new PathRef(
                            doc.getPathAsString())));
        } else {
            return new DocumentViewImpl(new DocumentLocationImpl(
                    documentManager.getRepositoryName(), null));
        }
    }

    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String loadPermanentLink(DocumentView docView)
            throws ClientException {
        restHelper.initContextFromRestRequest(docView);
        return "assets";
    }

    @Observer(value = { CONTENT_VIEW_PAGE_CHANGED_EVENT,
            CONTENT_VIEW_PAGE_SIZE_CHANGED_EVENT, CONTENT_VIEW_REFRESH_EVENT }, create = true)
    public void onContentViewPageProviderChanged(String contentViewName)
            throws ClientException {
        String currentContentViewName = getCurrentContentViewName();
        if (currentContentViewName != null
                && currentContentViewName.equals(contentViewName)) {
            String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
            // FIXME find a better way to update the current document only if we
            // are on DAM
            if ("/dam/assets.xhtml".equals(viewId)) {
                updateCurrentDocument();
            }
        }
    }

}
