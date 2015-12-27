/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.common.utils.UserAgentMatcher;

/**
 * Seam component used to outject a Session scoped flag that indicates if client's browser supports HTML5 (plugin free)
 * Drag&Drop feature
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Name("dndConfigHelper")
@Scope(ScopeType.EVENT)
public class DndConfigurationHelper {

    /**
     * Factory method used to push into the Session context a flag indicating if HTML5 Drag&Drop can be used
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
