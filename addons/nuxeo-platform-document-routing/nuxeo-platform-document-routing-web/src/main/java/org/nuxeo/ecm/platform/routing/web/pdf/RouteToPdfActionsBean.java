/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.routing.web.pdf;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Conversation;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

/**
 * Actions for generating a pdf for the current document route
 *
 * @author Mariana Cedica
 */
@Deprecated
@Name("routeToPdfActionBean")
@Scope(EVENT)
public class RouteToPdfActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public void doRenderView(String view_name) throws Exception {
        String base = BaseURL.getBaseURL(getHttpServletRequest());
        String url = base + view_name + "?conversationId="
                + Conversation.instance().getId();
        /**
         * hack needed for jboss 4
         */
        HttpServletResponse response = getHttpServletResponse();
        response.resetBuffer();
        response.sendRedirect(url);
        response.flushBuffer();
        getHttpServletRequest().setAttribute(
                NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, Boolean.TRUE);
        FacesContext.getCurrentInstance().responseComplete();
    }

    public int getLayoutColumnsCount(String layoutName) throws ClientException {
        WebLayoutManager wlm;
        try {
            wlm = Framework.getService(WebLayoutManager.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
        return wlm.getLayoutDefinition(layoutName).getRows().length;
    }

    private static HttpServletResponse getHttpServletResponse() {
        ServletResponse response = null;
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            response = (ServletResponse) facesContext.getExternalContext().getResponse();
        }

        if (response != null && response instanceof HttpServletResponse) {
            return (HttpServletResponse) response;
        }
        return null;
    }

    private static HttpServletRequest getHttpServletRequest() {
        ServletRequest request = null;
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            request = (ServletRequest) facesContext.getExternalContext().getRequest();
        }

        if (request != null && request instanceof HttpServletRequest) {
            return (HttpServletRequest) request;
        }
        return null;
    }
}
