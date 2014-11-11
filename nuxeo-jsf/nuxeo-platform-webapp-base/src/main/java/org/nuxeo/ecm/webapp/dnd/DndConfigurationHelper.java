/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.webapp.dnd;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.platform.web.common.UserAgentMatcher;

/**
 * Seam component used to outject a Session scoped flag that indicates if
 * client's browser supports HTML5 (plugin free) Drag&Drop feature
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
@Name("dndConfigHelper")
@Scope(ScopeType.EVENT)
public class DndConfigurationHelper {

    /**
     * Factory method used to push into the Session context a flag indicating if
     * HTML5 Drag&Drop can be used
     *
     * @return
     */
    @Factory(value = "useHtml5DragAndDrop", scope = ScopeType.SESSION)
    public boolean useHtml5DragAndDrop() {

        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext econtext = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) econtext.getRequest();

        String ua = request.getHeader("User-Agent");

        return UserAgentMatcher.html5DndIsSupported(ua);
    }

    public void setHtml5DndEnabled(boolean enabled) {
        Contexts.getSessionContext().set("useHtml5DragAndDrop", enabled);
    }

}
