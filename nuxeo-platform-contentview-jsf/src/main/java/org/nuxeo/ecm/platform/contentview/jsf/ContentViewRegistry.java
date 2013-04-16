/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.contentview.jsf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.ReferencePageProviderDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for content view contributions, handling accurate merge on hot
 * reload.
 *
 * @since 5.6
 */
public class ContentViewRegistry extends
        ContributionFragmentRegistry<ContentViewDescriptor> {

    protected static final Log log = LogFactory.getLog(ContentViewRegistry.class);

    protected final Map<String, ContentViewDescriptor> contentViews = new HashMap<String, ContentViewDescriptor>();

    protected final Map<String, Set<String>> contentViewsByFlag = new HashMap<String, Set<String>>();

    @Override
    public String getContributionId(ContentViewDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, ContentViewDescriptor contrib,
            ContentViewDescriptor newOrigContrib) {
        String name = contrib.getName();
        if (name == null) {
            log.error("Cannot register content view without a name");
            return;
        }
        if (contentViews.containsKey(id)) {
            contentViews.remove(id);
            removeContentViewFlags(name);
        }
        if (contrib.isEnabled()) {
            contentViews.put(name, contrib);
            addContentViewFlags(contrib);
            log.info("Registering content view with name " + id);
        }
    }

    @Override
    public void contributionRemoved(String id, ContentViewDescriptor origContrib) {
        contentViews.remove(id);
        removeContentViewFlags(origContrib);
        log.info("Unregistering content view with name " + id);
    }

    protected void addContentViewFlags(ContentViewDescriptor desc) {
        String name = desc.getName();
        List<String> flags = desc.getFlags();
        if (flags != null) {
            for (String flag : flags) {
                Set<String> items = contentViewsByFlag.get(flag);
                if (items == null) {
                    items = new HashSet<String>();
                }
                items.add(name);
                contentViewsByFlag.put(flag, items);
            }
        }
    }

    protected void removeContentViewFlags(String contentViewName) {
        for (Set<String> items : contentViewsByFlag.values()) {
            if (items != null) {
                items.remove(contentViewName);
            }
        }
    }

    protected void removeContentViewFlags(ContentViewDescriptor desc) {
        String name = desc.getName();
        List<String> flags = desc.getFlags();
        if (flags != null) {
            for (String flag : flags) {
                Set<String> items = contentViewsByFlag.get(flag);
                if (items != null) {
                    items.remove(name);
                }
            }
        }
    }

    @Override
    public ContentViewDescriptor clone(ContentViewDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(ContentViewDescriptor newDesc,
            ContentViewDescriptor oldDesc) {
        oldDesc.setEnabled(newDesc.isEnabled());

        String title = newDesc.getTitle();
        if (title != null) {
            oldDesc.title = title;
        }

        Boolean translateTitle = newDesc.getTranslateTitle();
        if (translateTitle != null) {
            oldDesc.translateTitle = translateTitle;
        }

        String emptySentence = newDesc.getEmptySentence();
        if (emptySentence != null) {
            oldDesc.emptySentence = emptySentence;
        }

        Boolean translateEmptySentence = newDesc.getTranslateEmptySentence();
        if (translateEmptySentence != null) {
            oldDesc.translateEmptySentence = translateEmptySentence;
        }

        String iconPath = newDesc.getIconPath();
        if (iconPath != null) {
            oldDesc.iconPath = iconPath;
        }

        List<String> actions = newDesc.getActionCategories();
        if (actions != null && !actions.isEmpty()) {
            oldDesc.actionCategories = actions;
        }

        String cacheKey = newDesc.getCacheKey();
        if (cacheKey != null) {
            oldDesc.cacheKey = cacheKey;
        }

        Integer cacheSize = newDesc.getCacheSize();
        if (cacheSize != null) {
            oldDesc.cacheSize = cacheSize;
        }

        CoreQueryPageProviderDescriptor coreDesc = newDesc.getCoreQueryPageProvider();
        if (coreDesc != null && coreDesc.isEnabled()) {
            oldDesc.coreQueryPageProvider = coreDesc;
            // make sure other page providers are reset
            oldDesc.genericPageProvider = null;
            oldDesc.referencePageProvider = null;
        }

        GenericPageProviderDescriptor genDesc = newDesc.getGenericPageProvider();
        if (genDesc != null && genDesc.isEnabled()) {
            oldDesc.genericPageProvider = genDesc;
            // make sure other page providers are reset
            oldDesc.coreQueryPageProvider = null;
            oldDesc.referencePageProvider = null;
        }

        ReferencePageProviderDescriptor refDesc = newDesc.getReferencePageProvider();
        if (refDesc != null && refDesc.isEnabled()) {
            oldDesc.referencePageProvider = refDesc;
            // make sure other page providers are reset
            oldDesc.coreQueryPageProvider = null;
            oldDesc.genericPageProvider = null;
        }

        String pagination = newDesc.getPagination();
        if (pagination != null) {
            oldDesc.pagination = pagination;
        }

        List<String> events = newDesc.getRefreshEventNames();
        if (events != null && !events.isEmpty()) {
            oldDesc.refreshEventNames = events;
        }
        events = newDesc.getResetEventNames();
        if (events != null && !events.isEmpty()) {
            oldDesc.resetEventNames = events;
        }

        ContentViewLayoutImpl searchLayout = newDesc.getSearchLayout();
        if (searchLayout != null) {
            oldDesc.searchLayout = searchLayout;
        }

        List<ContentViewLayout> resultLayouts = newDesc.getResultLayouts();
        if (resultLayouts != null) {
            Boolean appendResultLayout = newDesc.getAppendResultLayouts();
            if (Boolean.TRUE.equals(appendResultLayout)
                    || resultLayouts.isEmpty()) {
                List<ContentViewLayout> allLayouts = new ArrayList<ContentViewLayout>();
                if (oldDesc.resultLayouts != null) {
                    allLayouts.addAll(oldDesc.resultLayouts);
                }
                allLayouts.addAll(resultLayouts);
                oldDesc.resultLayouts = allLayouts;
            } else {
                oldDesc.resultLayouts = resultLayouts;
            }
        }

        List<String> flags = newDesc.getFlags();
        if (flags != null && !flags.isEmpty()) {
            oldDesc.flags = flags;
        }

        String selectionList = newDesc.getSelectionListName();
        if (selectionList != null) {
            oldDesc.selectionList = selectionList;
        }

        Boolean useGlobalPageSize = newDesc.getUseGlobalPageSize();
        if (useGlobalPageSize != null) {
            oldDesc.useGlobalPageSize = useGlobalPageSize;
        }

        Boolean showTitle = newDesc.getShowTitle();
        if (showTitle != null) {
            oldDesc.showTitle = showTitle;
        }

        Boolean showPageSizeSelector = newDesc.getShowPageSizeSelector();
        if (showPageSizeSelector != null) {
            oldDesc.showPageSizeSelector = showPageSizeSelector;
        }

        Boolean showRefreshCommand = newDesc.getShowRefreshCommand();
        if (showRefreshCommand != null) {
            oldDesc.showRefreshCommand = showRefreshCommand;
        }

        Boolean showFilterForm = newDesc.getShowFilterForm();
        if (showFilterForm != null) {
            oldDesc.showFilterForm = showFilterForm;
        }

        String searchDocument = newDesc.getSearchDocumentBinding();
        if (searchDocument != null) {
            oldDesc.searchDocument = searchDocument;
        }

        String resultCols = newDesc.getResultColumnsBinding();
        if (resultCols != null) {
            oldDesc.resultColumns = resultCols;
        }
    }

    // API

    public ContentViewDescriptor getContentView(String id) {
        return contentViews.get(id);
    }

    public boolean hasContentView(String id) {
        return contentViews.containsKey(id);
    }

    public Set<String> getContentViewsByFlag(String flag) {
        return contentViewsByFlag.get(flag);
    }

    public Set<String> getContentViewNames() {
        return contentViews.keySet();
    }

    public Collection<ContentViewDescriptor> getContentViews() {
        return contentViews.values();
    }

}
