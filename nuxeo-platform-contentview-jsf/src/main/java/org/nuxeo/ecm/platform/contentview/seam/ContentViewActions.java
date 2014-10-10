/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.seam;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.jsf.JSFActionContext;
import org.nuxeo.ecm.platform.actions.seam.SeamActionContext;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewCache;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;

/**
 * Handles cache and refresh for named content views.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 * @since 5.4.2: moved to content-view-jsf module
 */
@Name("contentViewActions")
@Scope(CONVERSATION)
public class ContentViewActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected ContentViewRestActions contentViewRestActions;

    @In(create = true)
    protected ContentViewService contentViewService;

    protected final ContentViewCache cache = new ContentViewCache();

    protected Long globalPageSize;

    @In(create = true)
    private transient NuxeoPrincipal currentNuxeoPrincipal;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient WebActions webActions;

    protected ContentView currentContentView;

    /**
     * Returns the current global content view.
     * <p>
     * Current content view is usually the last one displayed on the page, but
     * this may change depending on calls to
     * {@link #setCurrentContentView(ContentView)}.
     */
    public ContentView getCurrentContentView() {
        return currentContentView;
    }

    /**
     * Sets the current global content view.
     *
     * @see #getCurrentGlobalPageSize()
     */
    public void setCurrentContentView(ContentView cv) {
        currentContentView = cv;
    }

    /**
     * Returns the global page size, or returns the page size on current
     * content view if set.
     */
    public Long getCurrentGlobalPageSize() {
        if (currentContentView != null
                && currentContentView.getUseGlobalPageSize()) {
            return currentContentView.getCurrentPageSize();
        }
        return globalPageSize;
    }

    /**
     * Sets the global page size, useful to set the value having the
     * appropriate selection set, see {@link #getCurrentContentView()}
     */
    public void setCurrentGlobalPageSize(Long pageSize) {
        globalPageSize = pageSize;
    }

    /**
     * Returns the global page size
     */
    public Long getGlobalPageSize() {
        return globalPageSize;
    }

    /**
     * Returns the list of available options for page size selections, for
     * given content view.
     * <p>
     * This method relies on a hard-coded set of available values, and adapts
     * it to the current content view page size and max page size information
     * to present more or less items from this list.
     *
     * @since 5.8
     */
    @SuppressWarnings("boxing")
    public List<SelectItem> getPageSizeOptions(ContentView cv) {
        List<SelectItem> items = new ArrayList<SelectItem>();
        if (cv == null) {
            return items;
        }
        List<Long> values = new ArrayList<Long>(Arrays.asList(5L, 10L, 20L,
                30L, 40L, 50L));
        if (cv.getUseGlobalPageSize()) {
            // add the global page size if not present
            Long globalSize = getGlobalPageSize();
            if (globalSize != null && globalSize > 0
                    && !values.contains(globalSize)) {
                values.add(globalSize);
            }
        }
        long maxPageSize = 0;
        PageProvider<?> pp = cv.getCurrentPageProvider();
        if (pp != null) {
            // include the actual page size of page provider if not present
            long ppsize = pp.getPageSize();
            if (ppsize > 0 && !values.contains(ppsize)) {
                values.add(Long.valueOf(ppsize));
            }
            maxPageSize = pp.getMaxPageSize();
        }
        Collections.sort(values);
        for (Long value : values) {
            // remove every element that would be larger than max page size
            if (maxPageSize > 0 && maxPageSize < value) {
                break;
            }
            items.add(new SelectItem(value));
        }
        // make sure the list is not empty
        if (items.isEmpty()) {
            if (maxPageSize > 0) {
                items.add(new SelectItem(maxPageSize));
            } else {
                items.add(new SelectItem(1));
            }
        }
        return items;
    }

    /**
     * Sets the global page size
     */
    public void setGlobalPageSize(Long pageSize) {
        globalPageSize = pageSize;
    }

    public ContentView getContentView(String name) throws ClientException {
        return getContentView(name, null);
    }

    /**
     * Returns content view with given name, or null if no content view with
     * this name is found.
     * <p>
     * If parameter searchDocumentModel is not null, it will be set on the
     * content view. If it is null and the content is using a provider that
     * needs it, a new document model is created and attached to it. This
     * document model is resolved from the binding put in the content view XML
     * definition, or from the document type in this definition if no binding
     * is set.
     * <p>
     * If not null, this content view is set as the current content view so
     * that subsequent calls to other methods can take information from it,
     * like {@link #getCurrentGlobalPageSize()}
     * <p>
     * The content view is put in a cache map so that it's not rebuilt at each
     * call. It is rebuilt when its cache key changes (if defined).
     *
     * @throws ClientException
     */
    public ContentView getContentView(String name,
            DocumentModel searchDocumentModel) throws ClientException {
        ContentView cView = cache.get(name);
        if (cView == null) {
            cView = contentViewService.getContentView(name);
            if (cView != null) {
                cache.add(cView);
            }
        }
        if (cView != null) {
            if (searchDocumentModel != null) {
                cView.setSearchDocumentModel(searchDocumentModel);
            }
            setCurrentContentView(cView);
        }
        return cView;
    }

    public ContentView getContentViewWithProvider(String name)
            throws ClientException {
        return getContentViewWithProvider(name, null, null, null, null);
    }

    public ContentView getContentViewWithProvider(String name,
            DocumentModel searchDocumentModel) throws ClientException {
        return getContentViewWithProvider(name, searchDocumentModel, null,
                null, null);
    }

    public ContentView getContentViewWithProvider(String name,
            DocumentModel searchDocumentModel, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage) throws ClientException {
        return getContentViewWithProvider(name, searchDocumentModel, sortInfos,
                pageSize, currentPage, (Object[]) null);
    }

    public ContentView getContentViewWithProvider(String name,
            DocumentModel searchDocumentModel, List<SortInfo> sortInfos,
            Long defaultPageSize, Long pageSize, Long currentPage)
            throws ClientException {
        return getContentViewWithProvider(name, searchDocumentModel, sortInfos,
                defaultPageSize, pageSize, currentPage, (Object[]) null);
    }

    /**
     * @since 5.6
     */
    public ContentView getContentViewWithProvider(String name,
            DocumentModel searchDocumentModel, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage, Object... params)
            throws ClientException {
        return getContentViewWithProvider(name, searchDocumentModel, sortInfos,
                Long.valueOf(-1), pageSize, currentPage, params);
    }

    /**
     * Helper method to retrieve a content view, taking care of initialization
     * of page provider according to parameters and current global page size.
     * <p>
     * This method is not public to avoid EL method resolution issues.
     */
    protected ContentView getContentViewWithProvider(String name,
            DocumentModel searchDocumentModel, List<SortInfo> sortInfos,
            Long defaultPageSize, Long pageSize, Long currentPage,
            Object... params) throws ClientException {
        ContentView cView = getContentView(name, searchDocumentModel);
        if (cView != null) {
            if (cView.getUseGlobalPageSize()) {
                cView.setCurrentPageSize(globalPageSize);
            }
            if (cView.getCurrentPageSize() == null && defaultPageSize != null
                    && defaultPageSize.longValue() >= 0) {
                cView.setCurrentPageSize(defaultPageSize);
            }
            // initialize provider
            cView.getPageProvider(searchDocumentModel, sortInfos, pageSize,
                    currentPage, params);
        }
        return cView;
    }

    /**
     * Restore a content view from the given parameters.
     * <p>
     * The content view is put in a cache map so that it's not rebuilt at each
     * call. It is rebuilt when its cache key changes (if defined).
     *
     * @since 5.7
     */
    public ContentView restoreContentView(String contentViewName,
            Long currentPage, Long pageSize, List<SortInfo> sortInfos,
            String jsonContentViewState) throws UnsupportedEncodingException,
            ClientException {
        ContentView cv = contentViewRestActions.restoreContentView(
                contentViewName, currentPage, pageSize, sortInfos,
                jsonContentViewState);
        cache.add(cv);
        return cv;
    }

    /**
     * Refreshes all content views that have declared the given seam event name
     * as a refresh event in their XML configuration.
     */
    @BypassInterceptors
    public void refreshOnSeamEvent(String seamEventName) {
        cache.refreshOnEvent(seamEventName);
    }

    /**
     * Resets all content views page providers that have declared the given
     * seam event name as a reset event in their XML configuration.
     */
    @BypassInterceptors
    public void resetPageProviderOnSeamEvent(String seamEventName) {
        cache.resetPageProviderOnEvent(seamEventName);
    }

    @BypassInterceptors
    public void refresh(String contentViewName) {
        cache.refresh(contentViewName, false);
    }

    @BypassInterceptors
    public void refreshAndRewind(String contentViewName) {
        cache.refresh(contentViewName, true);
    }

    @BypassInterceptors
    public void resetPageProvider(String contentViewName) {
        cache.resetPageProvider(contentViewName);
    }

    @BypassInterceptors
    public void reset(String contentViewName) {
        cache.reset(contentViewName);
    }

    @BypassInterceptors
    public void resetAllContent() {
        cache.resetAllContent();
    }

    @BypassInterceptors
    public void resetAll() {
        cache.resetAll();
    }

    /**
     * @since 5.7
     */
    @BypassInterceptors
    public void refreshAll() {
        cache.refreshAll();
    }

    /**
     * @since 5.7
     */
    @BypassInterceptors
    public void refreshAndRewindAll() {
        cache.refreshAndRewindAll();
    }

    /**
     * Returns actions filtered depending on given custom context.
     * <p>
     * Boolean values are declared as objects to avoid conversion to "false"
     * when variable is not defined, and keep "null" value.
     *
     * @since 5.9.6
     */
    public List<Action> getActionsList(String category,
            DocumentModel currentDocument, ContentView contentView,
            Object showPageSizeSelector, Object showRefreshCommand,
            Object showCSVExport, Object showPDFExport,
            Object showSyndicationLinks, Boolean showSlideshow,
            Boolean showEditColumns, Boolean showSpreadsheet) {
        return webActions.getActionsList(
                category,
                createContentViewActionContext(currentDocument, contentView,
                        showPageSizeSelector, showRefreshCommand,
                        showCSVExport, showPDFExport, showSyndicationLinks,
                        showSlideshow, showEditColumns, showSpreadsheet));
    }

    protected ActionContext createContentViewActionContext(
            DocumentModel currentDocument, ContentView contentView,
            Object showPageSizeSelector, Object showRefreshCommand,
            Object showCSVExport, Object showPDFExport,
            Object showSyndicationLinks, Boolean showSlideshow,
            Boolean showEditColumns, Boolean showSpreadsheet) {
        ActionContext ctx;
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            ctx = new SeamActionContext();
        } else {
            ctx = new JSFActionContext(faces);
        }
        ctx.setCurrentPrincipal(currentNuxeoPrincipal);
        ctx.setDocumentManager(documentManager);
        ctx.setCurrentDocument(currentDocument);
        ctx.putLocalVariable("SeamContext", new SeamContextHelper());
        ctx.putLocalVariable("contentView", contentView);
        // additional local variables for action filters
        ctx.putLocalVariable("showPageSizeSelector", showPageSizeSelector);
        ctx.putLocalVariable("showRefreshCommand", showRefreshCommand);
        ctx.putLocalVariable("showCSVExport", showCSVExport);
        ctx.putLocalVariable("showPDFExport", showPDFExport);
        ctx.putLocalVariable("showSyndicationLinks", showSyndicationLinks);
        ctx.putLocalVariable("showSlideshow", showSlideshow);
        ctx.putLocalVariable("showEditColumns", showEditColumns);
        ctx.putLocalVariable("showSpreadsheet", showSpreadsheet);
        return ctx;
    }
}
