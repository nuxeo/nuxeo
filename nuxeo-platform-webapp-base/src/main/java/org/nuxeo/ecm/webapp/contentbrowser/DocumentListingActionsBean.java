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
package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.cache.LRUCachingMap;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Manages document listings rendering.
 *
 * @author Anahide Tchertchian
 */
@Name("documentListingActions")
@Scope(CONVERSATION)
public class DocumentListingActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentListingActionsBean.class);

    public static String DEFAULT_LISTING_LAYOUT = "document_listing";

    @In(create = true)
    protected transient NavigationContext navigationContext;

    // XXX: make max items configurable?
    protected LRUCachingMap<String, String> docTolistings = new LRUCachingMap<String, String>(
            20);

    protected String currentListingLayoutName = null;

    protected List<String> currentAvailableListingLayoutNames = null;

    public String getLayoutForDocument(DocumentModel doc) {
        if (doc != null) {
            String id = doc.getId();
            if (docTolistings.containsKey(id)) {
                return docTolistings.get(id);
            }
            List<String> availableLayouts = getAvailableLayoutsForDocument(doc);
            if (availableLayouts != null && !availableLayouts.isEmpty()) {
                return availableLayouts.get(0);
            }
        }
        return DEFAULT_LISTING_LAYOUT;
    }

    public void setLayoutForDocument(DocumentModel doc, String layoutName) {
        if (doc == null) {
            log.error("Cannot set listing layout for null document");
            return;
        }
        String id = doc.getId();
        docTolistings.put(id, layoutName);
        currentListingLayoutName = layoutName;
    }

    @Factory(value = "currentListingLayoutName", scope = EVENT)
    public String getLayoutForCurrentDocument() {
        if (currentListingLayoutName == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            currentListingLayoutName = getLayoutForDocument(currentDocument);
        }
        return currentListingLayoutName;
    }

    public void setLayoutForCurrentDocument(String layoutName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        setLayoutForDocument(currentDocument, layoutName);
    }

    public List<String> getAvailableLayoutsForDocument(DocumentModel doc) {
        if (doc == null) {
            return Collections.emptyList();
        }
        TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
        String[] layoutNames = typeInfo.getLayouts(BuiltinModes.LISTING, null);
        List<String> res = new ArrayList<String>();
        if (layoutNames != null && layoutNames.length > 0) {
            res.addAll(Arrays.asList(layoutNames));
        } else {
            res.add(DEFAULT_LISTING_LAYOUT);
        }
        return res;
    }

    @Factory(value = "currentAvailableListingLayoutNames", scope = EVENT)
    public List<String> getAvailableLayoutsForCurrentDocument() {
        if (currentAvailableListingLayoutNames == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            currentAvailableListingLayoutNames = getAvailableLayoutsForDocument(currentDocument);
        }
        return currentAvailableListingLayoutNames;
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create = false, inject = false)
    @BypassInterceptors
    public void documentChanged() {
        currentListingLayoutName = null;
        currentAvailableListingLayoutNames = null;
    }
}
