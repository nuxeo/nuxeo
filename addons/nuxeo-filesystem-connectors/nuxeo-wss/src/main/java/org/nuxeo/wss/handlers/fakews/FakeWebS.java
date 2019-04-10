/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.wss.handlers.fakews;

import java.io.IOException;
import java.util.List;

import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.servlet.WSSResponse;
import org.nuxeo.wss.spi.Backend;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.Site;
import org.nuxeo.wss.url.WSSUrlMapper;
import org.nuxeo.wss.url.WSSUrlMapping;

public class FakeWebS implements FakeWSHandler {

    public static final String pageUrl_TAG = "pageUrl";

    public void handleRequest(FakeWSRequest request, WSSResponse response) throws WSSException {

        response.addRenderingParameter("siteRoot", request.getSitePath());
        response.addRenderingParameter("request", request);

        String action = request.getAction();
        if ("http://schemas.microsoft.com/sharepoint/soap/WebUrlFromPageUrl".equals(action)) {
            String pageUrl;
            try {
                pageUrl = new FakeWSCmdParser(pageUrl_TAG).getParameter(request);
            } catch (IOException e) {
                throw new WSSException("Error parsing envelope", e);
            }

            response.setContentType("text/xml");

            WSSBackend backend = Backend.get(request);
            WSSUrlMapping mapping = WSSUrlMapper.getWebMapping(request, pageUrl);
            Site site = backend.getSite(mapping.getResourceUrl());
            String siteUrl = mapping.getSiteUrl() + site.getAccessUrl();

            response.addRenderingParameter("siteUrl", siteUrl);
            response.setRenderingTemplateName("WebUrlFromPageUrlResponse.ftl");

        } else if ("http://schemas.microsoft.com/sharepoint/soap/GetWebCollection".equals(action)) {
            response.setContentType("text/xml");
            WSSBackend backend = Backend.get(request);
            List<WSSListItem> items = backend.listItems("/");

            response.addRenderingParameter("sites", items);
            response.setRenderingTemplateName("GetWebCollection.ftl");

        } else {
            throw new WSSException("no FakeWS implemented for action " + action);
        }
    }

    protected String getSiteUrl(WSSRequest request, String pageUrl) {
        // only one site

        WSSUrlMapping mapping = WSSUrlMapper.getWebMapping(request, pageUrl);

        return mapping.getSiteUrl();

        // String siteUrl = request.getBaseUrl();
        // return siteUrl;
    }

}
