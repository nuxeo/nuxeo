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

package org.nuxeo.wss.handlers.fprpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.fprpc.FPError;
import org.nuxeo.wss.fprpc.FPRPCCall;
import org.nuxeo.wss.fprpc.FPRPCRequest;
import org.nuxeo.wss.fprpc.FPRPCResponse;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.url.WSSUrlMapper;

public class AuthorHandler extends AbstractFPRPCHandler implements FPRPCHandler {

    private static final Log log = LogFactory.getLog(AuthorHandler.class);

    @Override
    protected void processCall(FPRPCRequest request, FPRPCResponse fpResponse,
                               int callIndex, WSSBackend backend) throws WSSException {

        FPRPCCall call = request.getCalls().get(callIndex);
        String methodName = call.getMethodName();
        Map<String, String> parameters = call.getParameters();

        fpResponse.addRenderingParameter("siteRoot", request.getSitePath());
        fpResponse.addRenderingParameter("request", request);
        fpResponse.addRenderingParameter("serviceName", parameters.get("service_name"));

        log.debug("Handling FP Author call on method " + methodName);

        if ("open service".equals(methodName)) {
            fpResponse.setRenderingTemplateName("open-service.ftl");

        } else if ("list documents".equals(methodName)) {

            String location = parameters.get("initialUrl").trim();
            location = WSSUrlMapper.getUrlWithSitePath(request, location);

            boolean listFiles = false;
            boolean listFolders = false;
            boolean listIncludeParent = false;
            List<WSSListItem> folders = null;
            List<WSSListItem> files = null;
            WSSListItem parent = null;

            if ("true".equals(parameters.get("listFiles"))) {
                listFiles = true;
                files = backend.listLeafItems(location);
            }
            if ("true".equals(parameters.get("listFolders"))) {
                listFolders = true;
                folders = backend.listFolderishItems(location);
            }
            if ("true".equals(parameters.get("listIncludeParent"))) {
                listIncludeParent = true;
                parent = backend.getItem(location);
            }

            fpResponse.addRenderingParameter("listFiles", listFiles);
            fpResponse.addRenderingParameter("listFolders", listFolders);
            fpResponse.addRenderingParameter("listIncludeParent", listIncludeParent);
            fpResponse.addRenderingParameter("folders", folders);
            fpResponse.addRenderingParameter("files", files);
            fpResponse.addRenderingParameter("parent", parent);

            fpResponse.setRenderingTemplateName("list-documents.ftl");

        } else if ("get document".equals(methodName)) {
            String location = parameters.get("document_name");
            location = WSSUrlMapper.getUrlWithSitePath(request, location);

            WSSListItem doc = backend.getItem(location);

            if (doc == null) {
                try {
                    fpResponse.getHttpResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
                    fpResponse.getHttpResponse().flushBuffer();
                } catch (IOException e) {
                    log.error("Error handling error page", e);
                }
                fpResponse.setProcessed(true);
                return;
            }

            if ("chkoutExclusive".equalsIgnoreCase(parameters.get("get_option"))) {
                if (doc.canCheckOut(request.getUserName())) {
                    doc.checkOut(request.getUserName());
                    fpResponse.addRenderingParameter("doc", doc);
                    fpResponse.setRenderingTemplateName("get-document.ftl");
                    fpResponse.addBinaryStream(doc.getStream());
                } else {
                    if (doc.isCheckOut()) {
                        fpResponse.sendFPError(request, FPError.AlreadyLocked, getLockErrorMessage(doc));
                    } else {
                        fpResponse.sendFPError(request, FPError.AccessDenied, doc.getDisplayName());
                    }
                }
            } else {
                fpResponse.addRenderingParameter("doc", doc);
                fpResponse.setRenderingTemplateName("get-document.ftl");
                fpResponse.addBinaryStream(doc.getStream());
            }

        } else if ("put document".equals(methodName)) {

            String url = parameters.get("document/document_name");
            String location = WSSUrlMapper.getUrlWithSitePath(request, url);
            WSSListItem doc;
            String fileName;

            if (backend.exists(location)) {
                doc = backend.getItem(location);
                String[] urlParts = url.split("/");
                fileName = urlParts[urlParts.length - 1];
            } else {
                String[] urlParts = url.split("/");
                String newFileName = urlParts[urlParts.length - 1];
                String parentPath = (url + "*").replace("/" + newFileName + "*", "");
                String parentLocation = WSSUrlMapper.getUrlWithSitePath(request, parentPath);
                doc = backend.createFileItem(parentLocation, newFileName);
                fileName = newFileName;
            }

            if (doc == null) {
                try {
                    fpResponse.getHttpResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
                    fpResponse.getHttpResponse().flushBuffer();
                } catch (IOException e) {
                    log.error("Error handling error page", e);
                }
                fpResponse.setProcessed(true);
                return;
            }
            doc.setStream(request.getVermeerBinary(), fileName);
            fpResponse.addRenderingParameter("doc", doc);
            fpResponse.setRenderingTemplateName("put-document.ftl");

        } else if ("checkout document".equals(methodName)) {
            String location = parameters.get("document_name");
            location = WSSUrlMapper.getUrlWithSitePath(request, location);

            if (!backend.exists(location)) {
                fpResponse.sendFPError(request, FPError.UrlDoesNotExists, location);
                return;
            }

            WSSListItem doc = backend.getItem(location);
            fpResponse.addRenderingParameter("request", request);

            if (doc.canCheckOut(request.getUserName())) {
                doc.checkOut(request.getUserName());
                fpResponse.addRenderingParameter("doc", doc);
                fpResponse.setRenderingTemplateName("checkout-document.ftl");
            } else {
                if (doc.isCheckOut()) {
                    fpResponse.sendFPError(request, FPError.AlreadyLocked, getLockErrorMessage(doc));
                } else {
                    fpResponse.sendFPError(request, FPError.AccessDenied, doc.getDisplayName());
                }
                return;
            }

        } else if ("uncheckout document".equals(methodName)) {
            String location = parameters.get("document_name");
            location = WSSUrlMapper.getUrlWithSitePath(request, location);

            if (!backend.exists(location)) {
                fpResponse.sendFPError(request, FPError.UrlDoesNotExists, location);
                return;
            }

            WSSListItem doc = backend.getItem(location);

            if (doc.canUnCheckOut(request.getUserName())) {
                doc.uncheckOut(request.getUserName());
                fpResponse.addRenderingParameter("doc", doc);
                fpResponse.setRenderingTemplateName("uncheckout-document.ftl");
            } else {
                if (!doc.isCheckOut()) {
                    fpResponse.sendFPError(request, FPError.NotCheckedOut, doc.getDisplayName());
                } else {
                    fpResponse.sendFPError(request, FPError.AccessDenied, doc.getDisplayName());
                }
                return;
            }

        } else if ("create url-directories".equals(methodName)
                || "create url-directory".equals(methodName)) {
            String location;
            if ("create url-directories".equals(methodName)) {
                String urls = parameters.get("urldirs");
                // assume only one url
                urls = urls.substring(1, urls.length() - 1);
                Map<String, String> params = unpackMapValues(urls);
                location = params.get("url");
            } else {
                location = parameters.get("url");
            }
            String[] urlParts = location.split("/");
            String newFolderName = urlParts[urlParts.length - 1];
            String parentPath = (location + "*").replace("/" + newFolderName + "*", "");
            String parentLocation = WSSUrlMapper.getUrlWithSitePath(request, parentPath);

            WSSListItem folder = backend.createFolder(parentLocation, newFolderName);
            fpResponse.addRenderingParameter("folder", folder);
            fpResponse.setRenderingTemplateName("create-url-directories.ftl");

        } else if ("move document".equals(methodName)) {
            String location = parameters.get("oldUrl");
            String newLocation = parameters.get("newUrl");

            location = WSSUrlMapper.getUrlWithSitePath(request, location);
            newLocation = WSSUrlMapper.getUrlWithSitePath(request, newLocation);

            if (!backend.exists(location)) {
                fpResponse.sendFPError(request, FPError.UrlDoesNotExists, location);
                return;
            }

            WSSListItem doc = backend.moveItem(location, newLocation);
            fpResponse.addRenderingParameter("doc", doc);
            fpResponse.addRenderingParameter("oldUrl", location);
            fpResponse.addRenderingParameter("newUrl", location);
            fpResponse.setRenderingTemplateName("move-document.ftl");

        } else if ("remove documents".equals(methodName)) {
            String urllist = parameters.get("url_list");
            List<String> urls = unpackValues(urllist);

            List<String> removedDocUrls = new ArrayList<String>();
            List<String> removedDirUrls = new ArrayList<String>();
            List<String> failedDocUrls = new ArrayList<String>();
            List<String> failedDirUrls = new ArrayList<String>();

            for (String url : urls) {
                String location = WSSUrlMapper.getUrlWithSitePath(request, url);

                if (backend.exists(location)) {
                    WSSListItem doc = backend.getItem(location);

                    try {
                        backend.removeItem(location);
                        if (doc.isFolderish()) {
                            removedDirUrls.add(url);
                        } else {
                            removedDocUrls.add(url);
                        }
                    }
                    catch (WSSException e) {
                        if (doc.isFolderish()) {
                            failedDirUrls.add(url);
                        } else {
                            failedDocUrls.add(url);
                        }
                    }
                } else {
                    failedDocUrls.add(url);
                }
            }

            fpResponse.addRenderingParameter("removedDocUrls", removedDocUrls);
            fpResponse.addRenderingParameter("removedDirUrls", removedDirUrls);
            fpResponse.addRenderingParameter("failedDocUrls", failedDocUrls);
            fpResponse.addRenderingParameter("failedDirUrls", failedDirUrls);

            fpResponse.setRenderingTemplateName("remove-documents.ftl");

        } else if ("getDocsMetaInfo".equals(methodName)) {

            List<WSSListItem> docs = new ArrayList<WSSListItem>();
            List<WSSListItem> folders = new ArrayList<WSSListItem>();
            List<String> failedUrls = new ArrayList<String>();

            String url = parameters.get("document_name");
            List<String> urls = new ArrayList<String>();

            if (url == null) {
                url = parameters.get("url_list");
                if (url.startsWith("[")) {
                    urls = unpackValues(url);
                } else {
                    urls.add(url);
                }
            } else {
                urls.add(url);
            }

            for (String location : urls) {
                if (location.startsWith("http")) {// MSO 2K7 only !!!
                    location = WSSUrlMapper.getLocationFromFullUrl(request, location);
                }
                location = WSSUrlMapper.getUrlWithSitePath(request, location);
                if (backend.exists(location)) {
                    WSSListItem doc = backend.getItem(location);
                    if (doc.isFolderish()) {
                        folders.add(doc);
                    } else {
                        docs.add(doc);
                    }
                } else {
                    String failedUrl = location;
                    failedUrl = failedUrl.replace(request.getSitePath(), "");
                    if (failedUrl.startsWith("/")) {
                        failedUrl = failedUrl.substring(1);
                    }
                    failedUrls.add(failedUrl);
                }
            }

            fpResponse.addRenderingParameter("docs", docs);
            if (failedUrls.size() > 0) {
                fpResponse.addRenderingParameter("includeFailedUrls", true);
            } else {
                fpResponse.addRenderingParameter("includeFailedUrls", false);
            }
            if (folders.size() > 0) {
                fpResponse.addRenderingParameter("includefolders", true);
            } else {
                fpResponse.addRenderingParameter("includefolders", false);
            }

            fpResponse.addRenderingParameter("includefiles", true);
            fpResponse.addRenderingParameter("folders", folders);
            fpResponse.addRenderingParameter("failedUrls", failedUrls);
            fpResponse.setRenderingTemplateName("getDocsMetaInfo.ftl");
        }
    }

    protected List<String> unpackValues(String packedParams) {
        List<String> values = new ArrayList<String>();
        packedParams = packedParams.substring(1, packedParams.length() - 1);
        String[] parts = packedParams.split("\\;");
        for (String part : parts) {
            values.add(part);
        }
        return values;
    }

    protected Map<String, String> unpackMapValues(String packedParams) {
        List<String> values = unpackValues(packedParams);
        Map<String, String> params = new HashMap<String, String>();

        for (String value : values) {
            String[] parts = value.split("=");
            params.put(parts[0], parts[1]);
        }
        return params;
    }

    protected String getLockErrorMessage(WSSListItem doc) {
        return "The file " + doc.getDisplayName()
                + " is checked out or locked for editing by "
                + doc.getCheckoutUser();
    }

}
