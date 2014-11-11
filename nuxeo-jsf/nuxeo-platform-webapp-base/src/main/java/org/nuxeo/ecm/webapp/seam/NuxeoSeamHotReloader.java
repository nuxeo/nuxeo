/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.webapp.seam;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

/**
 * Simple Seam bean to control the Reload Action
 *
 * @author tiry
 */
@Name("seamReload")
@Scope(EVENT)
@Install(precedence = FRAMEWORK)
public class NuxeoSeamHotReloader implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    private static final Log log = LogFactory.getLog(NuxeoSeamHotReloader.class);

    @Factory(value = "seamHotReloadIsEnabled", scope = ScopeType.APPLICATION)
    public boolean isHotReloadEnabled() {
        return SeamHotReloadHelper.isHotReloadEnabled();
    }

    public String doReload() {

        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }

        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        String bigDownloadURL = BaseURL.getBaseURL(request);
        bigDownloadURL += "restAPI/seamReload";

        try {
            response.resetBuffer();
            response.sendRedirect(bigDownloadURL);
            response.flushBuffer();
            request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY,
                    true);
            facesContext.responseComplete();
        } catch (Exception e) {
            log.error("Error during redirect", e);
        }
        return null;
    }

}
