/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     stan
 */

package org.nuxeo.ecm.webapp.webcontainer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.theme.Manager;

@Name("WebcontainerThemeActions")
@Scope(ScopeType.PAGE)
public class ThemeActionsBean implements ThemeActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ThemeActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public List<SelectItem> getAvailableThemes() {
        List<SelectItem> themes = new ArrayList<SelectItem>();
        for (String theme : Manager.getThemeManager().getThemeNames()) {
            themes.add(new SelectItem(theme, theme));
        }
        return themes;
    }

    public List<SelectItem> getAvailablePages(String theme) {
        List<SelectItem> pages = new ArrayList<SelectItem>();
        if (theme != null && !theme.equals("")) {
            for (String pageName : Manager.getThemeManager().getPageNames(theme)) {
                pages.add(new SelectItem(pageName, pageName));
            }
        }
        return pages;
    }

}
