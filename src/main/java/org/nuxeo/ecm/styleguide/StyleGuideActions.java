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