/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.server.impl.atompub.AbstractAtomPubServiceCall;
import org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet;
import org.apache.chemistry.opencmis.server.shared.Dispatcher;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * Subclass CmisAtomPubServlet to inject a virtual-hosted base URL if needed.
 */
public class NuxeoCmisAtomPubServlet extends CmisAtomPubServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String baseUrl = VirtualHostHelper.getBaseURL(request);
        if (baseUrl != null) {
            baseUrl = StringUtils.stripEnd(baseUrl, "/")
                    + request.getServletPath() + "/"
                    + AbstractAtomPubServiceCall.REPOSITORY_PLACEHOLDER + "/";
            request.setAttribute(Dispatcher.BASE_URL_ATTRIBUTE, baseUrl);
        }
        super.service(request, response);
    }

}
