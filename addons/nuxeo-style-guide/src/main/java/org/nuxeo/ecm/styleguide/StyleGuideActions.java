/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.styleguide;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;

/**
 * @since 5.7
 */
@Name("styleGuideActions")
@Scope(CONVERSATION)
public class StyleGuideActions {

    public static final String PAGE_ACTION_CAT = "STYLE_GUIDE_PAGE";

    @In(create = true, required = false)
    protected transient ActionManager actionManager;

    protected Action currentPage;

    protected List<Action> pages;

    public Action getCurrentPage() {
        if (currentPage == null) {
            // initialize
            getPages();
            if (pages != null && !pages.isEmpty()) {
                currentPage = pages.get(0);
            }
        }
        return currentPage;
    }

    public void setCurrentPage(Action currentPage) {
        this.currentPage = currentPage;
    }

    public String getCurrentPageId() {
        Action currentPage = getCurrentPage();
        if (currentPage != null) {
            return currentPage.getId();
        }
        return null;
    }

    public void setCurrentPageId(String currentPageId) {
        this.currentPage = actionManager.getAction(currentPageId, null, true);
    }

    public List<Action> getPages() {
        if (pages == null) {
            pages = getActions(PAGE_ACTION_CAT);
        }
        return pages;
    }

    public List<Action> getActions(String cat) {
        return actionManager.getActions(cat, null);
    }

}
