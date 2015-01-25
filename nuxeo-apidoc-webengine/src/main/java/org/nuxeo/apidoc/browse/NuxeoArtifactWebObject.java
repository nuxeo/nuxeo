/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.apidoc.browse;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.doc.SimpleDocumentationItem;
import org.nuxeo.apidoc.documentation.DocumentationService;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

public abstract class NuxeoArtifactWebObject extends DefaultObject {

    protected String nxArtifactId;

    @Override
    protected void initialize(Object... args) {
        nxArtifactId = (String) args[0];
    }

    protected String getNxArtifactId() {
        return nxArtifactId;
    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }

    @Override
    public Template getView(String viewId) {
        return super.getView(viewId).arg(Distribution.DIST_ID, getDistributionId()).arg("enableDocumentationView",
                Boolean.TRUE);
    }

    public abstract NuxeoArtifact getNxArtifact();

    protected abstract Object doGet();

    protected String getDistributionId() {
        return (String) ctx.getProperty(Distribution.DIST_ID);
    }

    public AssociatedDocuments getAssociatedDocuments() {
        NuxeoArtifact nxItem = getNxArtifact();
        return nxItem.getAssociatedDocuments(ctx.getCoreSession());
    }

    @POST
    @Produces("text/html")
    @Path("updateDocumentation")
    public Object doUpdateDocumentation(DocumentationItem docItem) {
        if (!SecurityHelper.canEditDocumentation(getContext())) {
            throw new WebSecurityException("You are not allowed to do this operation");
        }

        DocumentationService ds = Framework.getLocalService(DocumentationService.class);

        ds.updateDocumentationItem(ctx.getCoreSession(), docItem);
        return redirect(getDocUrl());
    }

    protected String getDocUrl() {
        String path = getPath() + "/doc";
        // //TODO encode path segments if needed
        return path;
    }

    @Deprecated
    protected String computeUrl(String suffix) throws UnsupportedEncodingException {
        String targetUrl = ctx.getUrlPath();
        targetUrl = URLDecoder.decode(targetUrl, "ISO-8859-1");
        targetUrl = targetUrl.replace(ctx.getBasePath(), "");
        targetUrl = targetUrl.replace(suffix, "/doc");
        targetUrl = URLEncoder.encode(targetUrl, "ISO-8859-1");
        return targetUrl;
    }

    @POST
    @Produces("text/html")
    @Path("createDocumentation")
    public Object doCreateDocumentation(DocumentationItem docItem) {
        if (!SecurityHelper.canEditDocumentation(getContext())) {
            throw new WebSecurityException("You are not allowed to do this operation");
        }

        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        ds.createDocumentationItem(ctx.getCoreSession(), getNxArtifact(), docItem.getTitle(), docItem.getContent(),
                docItem.getType(), docItem.getApplicableVersion(), docItem.isApproved(), docItem.getRenderingType());
        return redirect(getDocUrl());
    }

    @POST
    @Produces("text/html")
    @Path("deleteDocumentation")
    public Object doDeleteDocumentation(@FormParam("uuid") String uuid) {
        if (!SecurityHelper.canEditDocumentation(getContext())) {
            throw new WebSecurityException("You are not allowed to do this operation");
        }
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        ds.deleteDocumentationItem(ctx.getCoreSession(), uuid);
        return redirect(getDocUrl());
    }

    @GET
    @Produces("text/html")
    public Object doViewDefault() {
        NuxeoArtifact nxItem = getNxArtifact();
        AssociatedDocuments docs = nxItem.getAssociatedDocuments(ctx.getCoreSession());
        return getView("default").arg("nxItem", nxItem).arg("docs", docs).arg("selectedTab", "defView");
    }

    @GET
    @Produces("text/html")
    @Path("doc")
    public Object doViewDoc() {
        NuxeoArtifact nxItem = getNxArtifact();
        AssociatedDocuments docs = nxItem.getAssociatedDocuments(ctx.getCoreSession());
        return getView("../documentation").arg("nxItem", nxItem).arg("docs", docs).arg("selectedTab", "docView");
    }

    @GET
    @Produces("text/html")
    @Path("aggregated")
    public Object doViewAggregated() {
        NuxeoArtifact nxItem = getNxArtifact();
        AssociatedDocuments docs = nxItem.getAssociatedDocuments(ctx.getCoreSession());
        return getView("aggregated").arg("nxItem", nxItem).arg("docs", docs).arg("selectedTab", "aggView");
    }

    @GET
    @Produces("text/html")
    @Path("createForm")
    public Object doAddDoc(@QueryParam("inline") Boolean inline, @QueryParam("type") String type) {
        NuxeoArtifact nxItem = getNxArtifact();
        List<String> versions = getSnapshotManager().getAvailableVersions(ctx.getCoreSession(), nxItem);
        DocumentationItem docItem = new SimpleDocumentationItem(nxItem);
        String targetView = "../docForm";
        if (inline != null && inline.equals(Boolean.TRUE)) {
            targetView = "../../docItemForm";
        }
        return getView(targetView).arg("nxItem", nxItem).arg("mode", "create").arg("docItem", docItem).arg("versions",
                versions).arg("selectedTab", "docView").arg("preselectedType", type);
    }

    @GET
    @Produces("text/html")
    @Path("editForm/{uuid}")
    public Object doEditDoc(@PathParam("uuid") String uuid) {
        NuxeoArtifact nxItem = getNxArtifact();
        List<String> versions = getSnapshotManager().getAvailableVersions(ctx.getCoreSession(), nxItem);
        DocumentModel existingDoc = ctx.getCoreSession().getDocument(new IdRef(uuid));
        DocumentationItem docItem = existingDoc.getAdapter(DocumentationItem.class);
        return getView("../docForm").arg("nxItem", nxItem).arg("mode", "edit").arg("docItem", docItem).arg("versions",
                versions).arg("selectedTab", "docView");
    }

    @GET
    @Produces("text/plain")
    @Path("quickEdit/{editId}")
    public Object quickEdit(@PathParam("editId") String editId) {

        if (editId == null || editId.startsWith("placeholder_")) {
            return "";
        }

        DocumentModel doc = getContext().getCoreSession().getDocument(new IdRef(editId));
        DocumentationItem item = doc.getAdapter(DocumentationItem.class);

        return item.getContent();
    }

    @POST
    @Produces("text/plain")
    @Path("quickEdit/{editId}")
    public Object quickEditSave(@PathParam("editId") String editId) {

        String title = getContext().getForm().getString("title");
        String content = getContext().getForm().getString("content");
        String type = getContext().getForm().getString("type");
        if (type == null || type.trim().length() == 0) {
            type = "description";
        }

        String renderingType = "wiki";
        if (content.contains("<ul>") || content.contains("<p>") || content.contains("<br/>")) {
            renderingType = "html";
        }

        List<String> applicableVersions = new ArrayList<String>();
        applicableVersions.add(getSnapshotManager().getSnapshot(getDistributionId(), getContext().getCoreSession()).getVersion()); // XXX
                                                                                                                                   // !!!
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        if (editId == null || editId.startsWith("placeholder_")) {
            ds.createDocumentationItem(getContext().getCoreSession(), getNxArtifact(), title, content, type,
                    applicableVersions, false, renderingType);
        } else {
            DocumentModel doc = getContext().getCoreSession().getDocument(new IdRef(editId));
            doc.setPropertyValue("dc:title", title);
            doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob(content));
            DocumentationItem item = doc.getAdapter(DocumentationItem.class);

            ds.updateDocumentationItem(getContext().getCoreSession(), item);
        }

        return "OK";
    }

    public Map<String, String> getCategories() {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        return ds.getCategories();
    }

}
