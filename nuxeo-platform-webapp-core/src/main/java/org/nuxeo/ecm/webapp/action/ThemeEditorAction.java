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
 *     Jean-Marc Orliaguet
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.action;

import java.io.Serializable;
import java.net.URL;
import java.util.Map;

import javax.ejb.Remove;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.theme.Manager;

@Name("themeEditorAction")
@Scope(ScopeType.STATELESS)
public class ThemeEditorAction implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ThemeEditorAction.class);

    public String startEditor() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
        Map<String, Object> requestMap = externalContext.getRequestMap();

        // Set the current theme
        URL themeUrl = (URL) requestMap.get("org.nuxeo.theme.url");
        String pagePath = Manager.getThemeManager().getPagePathByUrl(themeUrl);
        if (pagePath != null) {
            response.addCookie(createCookie("nxthemes.theme", pagePath));
        }

        // Switch to the editor
        response.addCookie(createCookie("nxthemes.engine", "editor"));
        return null;
    }

    private Cookie createCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        // expires when the browser is closed
        cookie.setMaxAge(-1);
        return cookie;
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

}
