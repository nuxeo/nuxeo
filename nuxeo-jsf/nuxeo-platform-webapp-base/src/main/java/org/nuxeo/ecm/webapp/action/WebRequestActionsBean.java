/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.runtime.api.Framework;

/**
 * Request-scoped bean caching actiosn retrieval, to avoid multiple retrieval of actions and avoid caching in the JSF
 * view.
 *
 * @since 8.2
 */
@Name("webRequestActions")
@Scope(ScopeType.EVENT)
public class WebRequestActionsBean {

    private static final Log log = LogFactory.getLog(WebRequestActionsBean.class);

    @In(create = true, required = false)
    protected transient ActionContextProvider actionContextProvider;

    protected Map<String, List<Action>> byCatCache = new HashMap<String, List<Action>>();

    protected Map<String, Action> byIdCache = new HashMap<String, Action>();

    protected Map<String, ActionContext> actionContexts = new HashMap<String, ActionContext>();

    public void reset() {
        if (log.isDebugEnabled()) {
            log.debug("reset");
        }
        byCatCache.clear();
        byIdCache.clear();
        actionContexts.clear();
    }

    protected ActionContext getCurrentActionContext() {
        // retrieve event-scoped context
        ActionContext ctx = (ActionContext) Component.getInstance("currentActionContext");
        String cacheKey = getActionContextCacheKey(ctx.getCurrentDocument());
        if (!actionContexts.containsKey(cacheKey)) {
            actionContexts.put(cacheKey, ctx);
        }
        return actionContexts.get(cacheKey);
    }

    protected ActionContext getActionContext(DocumentModel doc) {
        String cacheKey = getActionContextCacheKey(doc);
        if (!actionContexts.containsKey(cacheKey)) {
            actionContexts.put(cacheKey, actionContextProvider.createActionContext(doc));
        }
        return actionContexts.get(cacheKey);
    }

    protected String getActionContextCacheKey(DocumentModel doc) {
        return doc != null ? doc.getCacheKey() : null;
    }

    protected void fillContext(ActionContext ctx, Map<String, Object> oldVariables, String key, Object value) {
        oldVariables.put(key, ctx.putLocalVariable(key, value));
    }

    protected void cleanContext(ActionContext ctx, Map<String, Object> oldVariables) {
        if (oldVariables == null) {
            return;
        }
        for (Map.Entry<String, Object> item : oldVariables.entrySet()) {
            Object val = item.getValue();
            if (val == null) {
                ctx.removeLocalVariable(item.getKey());
            } else {
                ctx.putLocalVariable(item.getKey(), val);
            }
        }
    }

    public List<Action> getContentViewActions(String category, ContentView contentView, Object showPageSizeSelector,
            Object showRefreshCommand, Object showCSVExport, Object showPDFExport, Object showSyndicationLinks,
            Object showSlideshow, Object showEditColumns, Object showEditRows, Object showSpreadsheet) {
        ActionContext ctx = getCurrentActionContext();
        Map<String, Object> oldVariables = new HashMap<String, Object>();
        try {
            fillContext(ctx, oldVariables, "contentView", contentView);
            fillContext(ctx, oldVariables, "showPageSizeSelector", showPageSizeSelector);
            fillContext(ctx, oldVariables, "showRefreshCommand", showRefreshCommand);
            fillContext(ctx, oldVariables, "showCSVExport", showCSVExport);
            fillContext(ctx, oldVariables, "showPDFExport", showPDFExport);
            fillContext(ctx, oldVariables, "showSyndicationLinks", showSyndicationLinks);
            fillContext(ctx, oldVariables, "showSlideshow", showSlideshow);
            fillContext(ctx, oldVariables, "showEditColumns", showEditColumns);
            fillContext(ctx, oldVariables, "showEditRows", showEditRows);
            fillContext(ctx, oldVariables, "showSpreadsheet", showSpreadsheet);
            String cacheKey = "cv_" + (contentView != null ? contentView.getName() : "null") + "_" + category + "_"
                    + (contentView != null ? contentView.getCacheKey() : "null");
            return getActions(cacheKey, category, ctx, false, false);
        } finally {
            cleanContext(ctx, oldVariables);
        }
    }

    public List<Action> getContentViewSelectionActions(String category, ContentView contentView,
            List<Object> selectedEntries) {
        ActionContext ctx = getCurrentActionContext();
        Map<String, Object> oldVariables = new HashMap<String, Object>();
        try {
            fillContext(ctx, oldVariables, "contentView", contentView);
            fillContext(ctx, oldVariables, "selectedDocuments", selectedEntries);
            String cacheKey = "cv_sel_" + (contentView != null ? contentView.getName() : "null") + "_" + category + "_"
                    + (contentView != null ? contentView.getCacheKey() : "null");
            return getActions(cacheKey, category, ctx, false, false);
        } finally {
            cleanContext(ctx, oldVariables);
        }
    }

    public List<Action> getDocumentActions(String category, DocumentModel document, boolean removeFiltered,
            boolean postFilter) {
        ActionContext context = postFilter ? null : getActionContext(document);
        String cacheKey = "docs_" + category + "_" + (document != null ? document.getCacheKey() : "null") + "_"
                + String.valueOf(removeFiltered) + "_" + String.valueOf(postFilter);
        return getActions(cacheKey, category, context, removeFiltered, postFilter);
    }

    public Action getDocumentAction(String actionId, DocumentModel document, boolean removeFiltered,
            boolean postFilter) {
        ActionContext context = postFilter ? null : getActionContext(document);
        String cacheKey = "doc_" + actionId + "_" + (document != null ? document.getCacheKey() : "null") + "_"
                + String.valueOf(removeFiltered) + "_" + String.valueOf(postFilter);
        return getAction(cacheKey, actionId, context, removeFiltered, postFilter);
    }

    public List<Action> getActions(String cacheKey, String category, ActionContext context, boolean removeFiltered,
            boolean postFilter) {
        if (byCatCache.containsKey(cacheKey)) {
            return byCatCache.get(cacheKey);
        }
        List<Action> list = new ArrayList<Action>();
        List<String> categories = new ArrayList<String>();
        if (category != null) {
            String[] split = category.split(",|\\s");
            if (split != null) {
                for (String item : split) {
                    if (!StringUtils.isBlank(item)) {
                        categories.add(item.trim());
                    }
                }
            }
        }
        ActionManager actionManager = Framework.getService(ActionManager.class);
        for (String cat : categories) {
            List<Action> actions;
            if (postFilter) {
                actions = actionManager.getAllActions(cat);
            } else {
                actions = actionManager.getActions(cat, context, removeFiltered);
            }
            if (actions != null) {
                list.addAll(actions);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("caching for '" + cacheKey + "': " + list);
        }
        byCatCache.put(cacheKey, list);
        return list;
    }

    public Action getAction(String cacheKey, String actionId, ActionContext context, boolean removeFiltered,
            boolean postFilter) {
        if (byIdCache.containsKey(cacheKey)) {
            return byIdCache.get(cacheKey);
        }
        ActionManager actionManager = Framework.getService(ActionManager.class);
        Action action;
        if (postFilter) {
            action = actionManager.getAction(actionId);
        } else {
            action = actionManager.getAction(actionId, context, removeFiltered);
        }
        if (log.isDebugEnabled()) {
            log.debug("caching for '" + cacheKey + "': " + action);
        }
        byIdCache.put(cacheKey, action);
        return action;

    }

    public boolean isDocumentActionAvailable(Action action, DocumentModel document) {
        // return isActionAvailable(action, createActionContext(document));
        return false;
    }

    public boolean isActionAvailable(Action action, ActionContext context) {
        if (action.isFiltered()) {
            return action.getAvailable();
        }
        ActionManager actionManager = Framework.getService(ActionManager.class);
        return actionManager.checkFilters(action, context);
    }

}
