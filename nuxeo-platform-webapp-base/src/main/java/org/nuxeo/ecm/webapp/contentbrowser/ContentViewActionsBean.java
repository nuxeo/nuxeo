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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentView;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentViewService;

/**
 * Seam component handling cache and refresh for named content views.
 *
 * @author Anahide Tchertchian
 */
@Name("contentViewActions")
@Scope(CONVERSATION)
public class ContentViewActionsBean implements ContentViewActions {

    @In(create = true)
    protected ContentViewService contentViewService;

    protected Map<String, String> cacheKeys;

    protected Map<String, ContentView> contentViews;

    @In(create = true, required = false)
    protected FacesContext facesContext;

    protected void init() {
        if (cacheKeys == null) {
            cacheKeys = new HashMap<String, String>();
        }
        if (contentViews == null) {
            contentViews = new HashMap<String, ContentView>();
        }
    }

    public ContentView getContentView(String name) throws ClientException {
        init();
        ContentView cView;
        if (!contentViews.containsKey(name)) {
            cView = contentViewService.getContentView(name);
            if (cView != null) {
                contentViews.put(name, cView);
                cacheKeys.put(name, cView.getCacheKey());
            }
        } else {
            // check cache
            cView = contentViews.get(name);
            String oldCacheKey = cacheKeys.get(name);
            String newCacheKey = cView.getCacheKey();
            if (newCacheKey != null && !newCacheKey.equals(oldCacheKey)) {
                // rebuild it
                cView = contentViewService.getContentView(name);
                if (cView != null) {
                    contentViews.put(name, cView);
                    cacheKeys.put(name, newCacheKey);
                }
            }
        }
        return cView;
    }

    public void refresh(String name) {
        init();
        contentViews.remove(name);
        cacheKeys.remove(name);
    }

    public void refreshOnSeamEvents(String seamEventName) {
        if (seamEventName != null && contentViews != null) {
            for (Map.Entry<String, ContentView> entry : contentViews.entrySet()) {
                ContentView cv = entry.getValue();
                List<String> eventNames = cv.getRefreshEventNames();
                if (eventNames != null && eventNames.contains(seamEventName)) {
                    refresh(cv.getName());
                }
            }
        }
    }

}
