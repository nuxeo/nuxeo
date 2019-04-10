/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.routing.web.pdf;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Conversation;
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

    public void doRenderView(String view_name) throws IOException {
        String base = BaseURL.getBaseURL(getHttpServletRequest());
        String url = base + view_name + "?conversationId=" + Conversation.instance().getId();
        /**
         * hack needed for jboss 4
         */
        HttpServletResponse response = getHttpServletResponse();
        response.resetBuffer();
        response.sendRedirect(url);
        response.flushBuffer();
        getHttpServletRequest().setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, Boolean.TRUE);
        FacesContext.getCurrentInstance().responseComplete();
    }

    public int getLayoutColumnsCount(String layoutName) {
        WebLayoutManager wlm = Framework.getService(WebLayoutManager.class);
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
