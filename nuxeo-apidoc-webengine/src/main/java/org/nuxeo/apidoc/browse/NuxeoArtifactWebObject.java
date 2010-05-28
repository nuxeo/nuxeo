/*
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
 *
 * $Id$
 */

package org.nuxeo.apidoc.browse;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public abstract class NuxeoArtifactWebObject extends DefaultObject {

    public static final String DIST_ID = "distId";

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
        return super.getView(viewId).arg(DIST_ID, getDistributionId()).arg("enableDocumentationView", true);
    }

    public abstract NuxeoArtifact getNxArtifact();

    protected abstract Object doGet() throws Exception;

    protected String getDistributionId() {
        return (String)ctx.getProperty(DIST_ID);
    }

    public AssociatedDocuments getAssociatedDocuments() {
        NuxeoArtifact nxItem = getNxArtifact();
        return nxItem.getAssociatedDocuments(ctx.getCoreSession());
    }

    @POST
    @Produces("text/html")
    @Path(value = "updateDocumentation")
    public Object doUpdateDocumentation(DocumentationItem docItem) throws Exception {
        if (!SecurityHelper.canEditDocumentation(getContext()))
        {
            throw new WebSecurityException("You are not allowed to do this operation");
        }

        DocumentationService ds = Framework.getLocalService(DocumentationService.class);

        ds.updateDocumentationItem(ctx.getCoreSession(), docItem);
        return redirect(getDocUrl());
    }

    protected String getDocUrl() {
        String path = getPath()+"/doc";
//        //TODO encode path segments if needed
//        try {
//            StringBuilder buf = new StringBuilder();
//            org.nuxeo.common.utils.Path p = new org.nuxeo.common.utils.Path(path);
//            for (int i=0,len=p.segmentCount(); i<len; i++) {
//                buf.append("/").append(URLEncoder.encode(p.segment(i), "ISO-8859-1"));
//            }
//            path = buf.toString();
//        } catch (Exception e) {
//            throw WebException.wrap(e);
//        }
        return path;
    }

    @Deprecated
    protected String computeUrl(String suffix) throws Exception {
        String targetUrl = ctx.getUrlPath();
        targetUrl = URLDecoder.decode(targetUrl, "ISO-8859-1");
        targetUrl= targetUrl.replace(ctx.getBasePath(), "");
        targetUrl= targetUrl.replace(suffix, "/doc");
        targetUrl = URLEncoder.encode(targetUrl, "ISO-8859-1");
        return targetUrl;
    }

    @POST
    @Produces("text/html")
    @Path(value = "createDocumentation")
    public Object doCreateDocumentation(DocumentationItem docItem) throws Exception {
        if (!SecurityHelper.canEditDocumentation(getContext()))
        {
            throw new WebSecurityException("You are not allowed to do this operation");
        }

        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        ds.createDocumentationItem(ctx.getCoreSession(), getNxArtifact(), docItem.getTitle(), docItem.getContent(), docItem.getType(), docItem.getApplicableVersion(), docItem.isApproved(), docItem.getRenderingType());
        return redirect(getDocUrl());
    }

    @GET
    @Produces("text/html")
    @Path(value = "doc")
    public Object doViewDoc() throws Exception {
        NuxeoArtifact nxItem = getNxArtifact();
        AssociatedDocuments docs = nxItem.getAssociatedDocuments(ctx.getCoreSession());
        return getView("../documentation")
                .arg("nxItem", nxItem).arg("docs", docs).arg("selectedTab","docView");
    }

    @GET
    @Produces("text/html")
    public Object doViewAggregated() throws Exception {
        NuxeoArtifact nxItem = getNxArtifact();
        AssociatedDocuments docs = nxItem.getAssociatedDocuments(ctx.getCoreSession());
        return getView("aggregated")
                .arg("nxItem", nxItem).arg("docs", docs).arg("selectedTab","aggView");
    }

    @GET
    @Produces("text/html")
    @Path(value = "createForm")
    public Object doAddDoc(@QueryParam("inline") Boolean inline, @QueryParam("type") String type) throws Exception {
        NuxeoArtifact nxItem = getNxArtifact();
        List<String> versions = getSnapshotManager().getAvailableVersions(ctx.getCoreSession(), nxItem);
        DocumentationItem docItem = new SimpleDocumentationItem(nxItem);
        String targetView = "../docForm";
        if (inline!=null && inline.equals(true)) {
            targetView = "../../docItemForm";
        }
        return getView(targetView)
                .arg("nxItem", nxItem).arg("mode","create").arg("docItem", docItem).arg("versions", versions).arg("selectedTab","docView").arg("preselectedType",type);
    }

    @GET
    @Produces("text/html")
    @Path(value = "editForm/{uuid}")
    public Object doEditDoc(@PathParam("uuid") String uuid) throws Exception {
        NuxeoArtifact nxItem = getNxArtifact();
        List<String> versions = getSnapshotManager().getAvailableVersions(ctx.getCoreSession(), nxItem);
        DocumentModel existingDoc = ctx.getCoreSession().getDocument(new IdRef(uuid));
        DocumentationItem docItem = existingDoc.getAdapter(DocumentationItem.class);
        return getView("../docForm")
                .arg("nxItem", nxItem).arg("mode","edit").arg("docItem", docItem).arg("versions", versions).arg("selectedTab","docView");
    }

    @GET
    @Produces("text/plain")
    @Path(value = "quickEdit/{editId}")
    public Object quickEdit(@PathParam("editId") String editId) throws Exception {

        if (editId==null || editId.startsWith("placeholder_")) {
            return "";
        }

        DocumentModel doc = getContext().getCoreSession().getDocument(new IdRef(editId));
        DocumentationItem item = doc.getAdapter(DocumentationItem.class);

        return item.getContent();
    }

    @POST
    @Produces("text/plain")
    @Path(value = "quickEdit/{editId}")
    public Object quickEditSave(@PathParam("editId") String editId) throws Exception {


        String title = getContext().getForm().getString("title");
        String content = getContext().getForm().getString("content");
        String type = getContext().getForm().getString("type");
        if (type==null || type.trim().length()==0) {
            type="description";
        }

        String renderingType="wiki";
        if (content.contains("<ul>") || content.contains("<p>") || content.contains("<br/>")) {
            renderingType="html";
        }

        List<String> applicableVersions = new ArrayList<String>();
        applicableVersions.add(getSnapshotManager().getSnapshot(getDistributionId(), getContext().getCoreSession()).getVersion()); // XXX !!!
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        if (editId==null || editId.startsWith("placeholder_")) {
            ds.createDocumentationItem(getContext().getCoreSession(), getNxArtifact(), title, content, type, applicableVersions, false, renderingType);
        }
        else {
            DocumentModel doc = getContext().getCoreSession().getDocument(new IdRef(editId));
            doc.setPropertyValue("dc:title", title);
            doc.setPropertyValue("file:content", new StringBlob(content));
            DocumentationItem item = doc.getAdapter(DocumentationItem.class);

            ds.updateDocumentationItem(getContext().getCoreSession(), item);
        }

        return "OK";
    }



    public Map<String, String> getCategories() throws Exception {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        return ds.getCategories();
    }

}
