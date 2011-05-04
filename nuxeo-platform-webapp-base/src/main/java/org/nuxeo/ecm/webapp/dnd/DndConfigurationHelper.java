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

    protected static final String UA_PATTERN_FF36 = "Firefox/3.6";

    protected static final String UA_PATTERN_FF4 = "Firefox/4";

    protected static final String UA_PATTERN_CHROME = "Chrome/";

    protected static final String UA_PATTERN_CHROMIUM = "Chromium/";

    protected static final String UA_PATTERN_SAFARI = "Safari/";

    protected static final String UA_PATTERN_SAFARI_5 = "Version/5";

    /**
     * Factiry method used to push into the Session context a flag indicating if
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

        if (ua == null) {
            return false;
        }
        if (ua.contains(UA_PATTERN_FF36) || ua.contains(UA_PATTERN_FF4)) {
            return true;
        }
        if (ua.contains(UA_PATTERN_CHROME) || ua.contains(UA_PATTERN_CHROMIUM)) {
            return true;
        }
        if (ua.contains(UA_PATTERN_SAFARI) && ua.contains(UA_PATTERN_SAFARI_5)) {
            return true;
        }

        return false;
    }

    public void setHtml5DndEnabled(boolean enabled) {
        Contexts.getSessionContext().set("useHtml5DragAndDrop", enabled);
    }

}
