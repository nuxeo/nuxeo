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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.fm.FreeMarkerRenderer;
import org.nuxeo.wss.servlet.WSSResponse;
import org.nuxeo.wss.spi.Backend;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.DWSDocument;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.Link;
import org.nuxeo.wss.spi.dws.Task;
import org.nuxeo.wss.spi.dws.User;

/**
 * Minimal fake implementation of the DWS WebService. Uses FreeMarker to render responses.
 *
 * @author Thierry Delprat
 */
public class FakeDWS implements FakeWSHandler {

    public static final String document_TAG = "document";

    public static final String url_TAG = "url";

    public void handleRequest(FakeWSRequest request, WSSResponse response) throws WSSException {

        response.addRenderingParameter("siteRoot", request.getSitePath());
        response.addRenderingParameter("request", request);

        if ("http://schemas.microsoft.com/sharepoint/soap/dws/GetDwsMetaData".equals(request.getAction())
                || "http://schemas.microsoft.com/sharepoint/soap/dws/GetDwsData".equals(request.getAction())) {

            boolean withMeta = true;
            if ("http://schemas.microsoft.com/sharepoint/soap/dws/GetDwsData".equals(request.getAction())) {
                withMeta = false;
            }

            String documentUrl = null;

            try {
                documentUrl = new FakeWSCmdParser(document_TAG).getParameter(request);
                documentUrl = URLDecoder.decode(documentUrl, "UTF-8");
            } catch (IOException e) {
                throw new WSSException("Error parsing envelope", e);
            }

            response.setContentType("text/xml");

            String subPath = documentUrl.replace(request.getBaseUrl(), "");

            WSSBackend backend = Backend.get(request);
            WSSListItem item = backend.getItem(subPath);

            DWSMetaData metadata = backend.getMetaData(subPath, request);
            List<User> users = metadata.getUsers();
            List<Task> tasks = metadata.getTasks();
            List<Link> links = metadata.getLinks();
            List<User> assignees = new ArrayList<User>();
            List<DWSDocument> docs = new ArrayList<DWSDocument>();
            for (Task task : tasks) {
                String login = task.getAssigneeLogin();
                for (User user : users) {
                    if (user.getLogin().equals(login)) {
                        assignees.add(user);
                        break;
                    }
                }
            }
            for (Task task : tasks) {
                task.updateReferences(users, assignees);
            }
            for (Link link : links) {
                link.updateReferences(users);
            }
            int docIdx = 1;
            String siteRoot = calculateSiteRoot(request);
            for (WSSListItem doc : metadata.getDocuments()) {
                DWSDocument dwsd = new DWSDocument(doc, siteRoot);
                dwsd.updateReferences(users);
                dwsd.setId(String.valueOf(docIdx));
                docs.add(dwsd);
                docIdx++;
            }

            String siteUrl = metadata.getSite().getItem().getRelativeSubPath(siteRoot);

            Map<String, Object> renderingContext = new HashMap<String, Object>();
            renderingContext.put("doc", item);
            renderingContext.put("request", request);
            renderingContext.put("tasks", tasks);
            renderingContext.put("docs", docs);
            renderingContext.put("links", links);
            renderingContext.put("users", users);
            renderingContext.put("assignees", assignees);
            renderingContext.put("currentUser", metadata.getCurrentUser());
            renderingContext.put("site", metadata.getSite());
            renderingContext.put("updateTS", System.currentTimeMillis() + "");
            renderingContext.put("siteRoot", siteRoot);
            renderingContext.put("siteUrl", siteUrl);

            try {
                String xmlMetaData = "";
                if (withMeta) {
                    xmlMetaData = renderSubTemplate("GetDwsMetaDataBody.ftl", renderingContext);
                } else {
                    xmlMetaData = renderSubTemplate("GetDwsDataBody.ftl", renderingContext);
                }

                xmlMetaData = StringEscapeUtils.escapeXml(xmlMetaData);
                response.addRenderingParameter("doc", item);
                response.addRenderingParameter("xmlMetaData", xmlMetaData);
                if (withMeta) {
                    response.setRenderingTemplateName("GetDwsMetaData.ftl");
                } else {
                    response.setRenderingTemplateName("GetDwsData.ftl");
                }

            } catch (IOException e) {
                throw new WSSException("Error while rendering sub template", e);
            }
        } else if ("http://schemas.microsoft.com/sharepoint/soap/dws/CreateFolder".equals(request.getAction())) {

            String documentUrl;

            try {
                documentUrl = new FakeWSCmdParser(url_TAG).getParameter(request);
                documentUrl = URLDecoder.decode(documentUrl, "UTF-8");
            } catch (IOException e) {
                throw new WSSException("Error parsing envelope", e);
            }

            response.setContentType("text/xml");
            String siteRoot = calculateSiteRoot(request);
            String folderName;
            String targetPath;
            int idx = documentUrl.lastIndexOf("/");
            if (idx > 0) {
                folderName = documentUrl.substring(idx + 1);
                targetPath = siteRoot + "/" + documentUrl.substring(0, idx);
            } else {
                folderName = documentUrl;
                targetPath = siteRoot;
            }

            WSSBackend backend = Backend.get(request);
            backend.createFolder(targetPath, folderName);

            response.setRenderingTemplateName("CreateFolderResponse.ftl");
        } else {
            throw new WSSException("no FakeWS implemented for action " + request.getAction());
        }
    }

    protected String renderSubTemplate(String renderingTemplateName, Map<String, Object> renderingContext)
            throws IOException {
        Writer writer;
        ByteArrayOutputStream bufferedOs;
        bufferedOs = new ByteArrayOutputStream();
        writer = new BufferedWriter(new OutputStreamWriter(bufferedOs));
        FreeMarkerRenderer.instance().render(renderingTemplateName, renderingContext, writer);
        writer.flush();
        writer.close();
        return bufferedOs.toString("UTF-8");
    }

    protected String calculateSiteRoot(FakeWSRequest request) {
        String siteRoot = request.getSitePath();
        if (siteRoot == null || siteRoot.equals("")) {
            siteRoot = request.getHttpRequest().getContextPath();
        }
        if (siteRoot == null) { // happens in unit tests
            siteRoot = System.getProperty("org.nuxeo.ecm.contextPath", "/nuxeo");
        }
        if (!siteRoot.startsWith("/")) {
            siteRoot = "/" + siteRoot;
        }
        if (siteRoot.endsWith("/")) {
            siteRoot = siteRoot.substring(0, siteRoot.length() - 1);
        }
        return siteRoot;
    }

}
