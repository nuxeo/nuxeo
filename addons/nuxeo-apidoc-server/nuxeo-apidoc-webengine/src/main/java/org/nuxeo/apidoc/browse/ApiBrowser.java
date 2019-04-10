/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.DocumentationService;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.tree.TreeHelper;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerService;
import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializer;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.wikimodel.wem.WikiParserException;

@WebObject(type = "apibrowser")
public class ApiBrowser extends DefaultObject {

    protected String distributionId;

    protected boolean embeddedMode = false;

    protected SnapshotManager getSnapshotManager() {
        return Framework.getService(SnapshotManager.class);
    }

    protected ArtifactSearcher getSearcher() {
        return Framework.getService(ArtifactSearcher.class);
    }

    @Override
    protected void initialize(Object... args) {
        distributionId = (String) args[0];
        if (args.length > 1) {
            Boolean embed = (Boolean) args[1];
            embeddedMode = embed != null && embed;
        }
    }

    @GET
    @Produces("text/plain")
    @Path("tree")
    public Object tree(@QueryParam("root") String source) {
        return TreeHelper.updateTree(getContext(), source);
    }

    @GET
    @Produces("text/html")
    @Path("treeView")
    public Object treeView() {
        return getView("tree").arg(Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    public Object doGet() {
        if (embeddedMode) {
            DistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
            Map<String, Integer> stats = new HashMap<String, Integer>();
            stats.put("bundles", snap.getBundleIds().size());
            stats.put("jComponents", snap.getJavaComponentIds().size());
            stats.put("xComponents", snap.getXmlComponentIds().size());
            stats.put("services", snap.getServiceIds().size());
            stats.put("xps", snap.getExtensionPointIds().size());
            stats.put("contribs", snap.getComponentIds().size());
            return getView("indexSimple").arg(Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID)).arg("stats",
                    stats);
        } else {
            return getView("index").arg(Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID));
        }
    }

    @GET
    @Produces("text/html")
    @Path("listBundleGroups")
    public Object getMavenGroups() {
        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(getSnapshotManager().getSnapshot(distributionId,
                ctx.getCoreSession()));
        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        return getView("listBundleGroups").arg("tree", tree).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID));
    }

    public Map<String, DocumentationItem> getDescriptions(String targetType) {
        DocumentationService ds = Framework.getService(DocumentationService.class);
        return ds.getAvailableDescriptions(getContext().getCoreSession(), targetType);
    }

    @GET
    @Produces("text/html")
    @Path("listBundles")
    public Object getBundles() {
        List<String> bundleIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getBundleIds();
        return getView("listBundles").arg("bundleIds", bundleIds).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path("listComponents")
    public Object getComponents() {
        List<String> javaComponentIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getJavaComponentIds();
        List<ArtifactLabel> javaLabels = new ArrayList<ArtifactLabel>();
        for (String id : javaComponentIds) {
            javaLabels.add(ArtifactLabel.createLabelFromComponent(id));
        }

        List<String> xmlComponentIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getXmlComponentIds();
        List<ArtifactLabel> xmlLabels = new ArrayList<ArtifactLabel>();
        for (String id : xmlComponentIds) {
            xmlLabels.add(ArtifactLabel.createLabelFromComponent(id));
        }

        Collections.sort(javaLabels);
        Collections.sort(xmlLabels);

        return getView("listComponents").arg("javaComponents", javaLabels).arg("xmlComponents", xmlLabels).arg(
                Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path("listServices")
    public Object getServices() {
        List<String> serviceIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getServiceIds();

        List<ArtifactLabel> serviceLabels = new ArrayList<ArtifactLabel>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        Collections.sort(serviceLabels);

        return getView("listServices").arg("services", serviceLabels).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID));
    }

    protected Map<String, String> getRenderedDescriptions(String type) {

        Map<String, DocumentationItem> descs = getDescriptions(type);
        Map<String, String> result = new HashMap<String, String>();

        for (String key : descs.keySet()) {
            DocumentationItem docItem = descs.get(key);
            String content = docItem.getContent();
            if ("wiki".equals(docItem.getRenderingType())) {
                Reader reader = new StringReader(content);
                WikiSerializer engine = new WikiSerializer();
                StringWriter writer = new StringWriter();
                try {
                    engine.serialize(reader, writer);
                } catch (IOException | WikiParserException e) {
                    throw new NuxeoException(e);
                }
                content = writer.getBuffer().toString();
            } else {
                content = "<div class='doc'>" + content + "</div>";
            }
            result.put(key, content);
        }
        return result;
    }

    @GET
    @Produces("text/plain")
    @Path("feedServices")
    public String feedServices() throws JSONException {
        List<String> serviceIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getServiceIds();

        Map<String, String> descs = getRenderedDescriptions("NXService");

        List<ArtifactLabel> serviceLabels = new ArrayList<ArtifactLabel>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        Collections.sort(serviceLabels);

        JSONArray array = new JSONArray();

        for (ArtifactLabel label : serviceLabels) {
            JSONObject object = new JSONObject();
            object.put("id", label.getId());
            object.put("label", label.getLabel());
            object.put("desc", descs.get(label.id));
            object.put("url", "http://explorer.nuxeo.org/nuxeo/site/distribution/current/service2Bundle/" + label.id);
            array.put(object);
        }

        return array.toString();
    }

    @GET
    @Produces("text/plain")
    @Path("feedExtensionPoints")
    public String feedExtensionPoints() throws JSONException {
        List<String> epIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getExtensionPointIds();

        Map<String, String> descs = getRenderedDescriptions("NXExtensionPoint");

        List<ArtifactLabel> labels = new ArrayList<ArtifactLabel>();

        for (String id : epIds) {
            labels.add(ArtifactLabel.createLabelFromExtensionPoint(id));
        }
        Collections.sort(labels);

        JSONArray array = new JSONArray();

        for (ArtifactLabel label : labels) {
            JSONObject object = new JSONObject();
            object.put("id", label.getId());
            object.put("label", label.getLabel());
            object.put("desc", descs.get(label.id));
            object.put("url", "http://explorer.nuxeo.org/nuxeo/site/distribution/current/extensionPoint2Component/"
                    + label.id);
            array.put(object);
        }

        return array.toString();
    }

    @GET
    @Produces("text/html")
    @Path("listContributions")
    public Object getContributions() {
        DistributionSnapshot snapshot = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        List<String> cIds = snapshot.getContributionIds();
        return getView("listContributions").arg("contributions", snapshot.getContributions()).arg(
                Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path("listExtensionPoints")
    public Object getExtensionPoints() {
        List<String> epIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getExtensionPointIds();

        List<ArtifactLabel> labels = epIds.stream().map(ArtifactLabel::createLabelFromExtensionPoint).collect(Collectors.toList());

        Collections.sort(labels);
        return getView("listExtensionPoints").arg("eps", labels).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID));
    }

    /**
     * XXX Not used?
     */
    @POST
    @Produces("text/html")
    @Path("filterComponents")
    public Object filterComponents(@FormParam("fulltext") String fulltext) {
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ComponentInfo.TYPE_NAME, fulltext);

        List<ArtifactLabel> xmlLabels = new ArrayList<>();
        List<ArtifactLabel> javaLabels = new ArrayList<>();

        for (NuxeoArtifact item : artifacts) {
            ComponentInfo ci = (ComponentInfo) item;
            if (ci.isXmlPureComponent()) {
                xmlLabels.add(ArtifactLabel.createLabelFromComponent(ci.getId()));
            } else {
                javaLabels.add(ArtifactLabel.createLabelFromComponent(ci.getId()));
            }
        }
        return getView("listComponents").arg("javaComponents", javaLabels).arg("xmlComponents", xmlLabels).arg(
                Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID)).arg("searchFilter", sanitize(fulltext));
    }

    /**
     * XXX Not used?
     */
    @POST
    @Produces("text/html")
    @Path("filterBundles")
    public Object filterBundles(@FormParam("fulltext") String fulltext) {
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                BundleInfo.TYPE_NAME, fulltext);
        List<String> bundleIds = new ArrayList<String>();
        for (NuxeoArtifact item : artifacts) {
            bundleIds.add(item.getId());
        }
        return getView("listBundles").arg("bundleIds", bundleIds).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID)).arg("searchFilter", sanitize(fulltext));
    }

    /**
     * XXX Not used?
     */
    @POST
    @Produces("text/html")
    @Path("filterServices")
    public Object filterServices() {
        String fulltext = getContext().getForm().getFormProperty("fulltext");
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ServiceInfo.TYPE_NAME, fulltext);
        List<String> serviceIds = new ArrayList<String>();
        for (NuxeoArtifact item : artifacts) {
            serviceIds.add(item.getId());
        }
        List<ArtifactLabel> serviceLabels = new ArrayList<ArtifactLabel>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        return getView("listServices").arg("services", serviceLabels).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID)).arg("searchFilter", sanitize(fulltext));
    }

    @POST
    @Produces("text/html")
    @Path("filterExtensionPoints")
    public Object filterExtensionPoints(@FormParam("fulltext") String fulltext) {
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ExtensionPointInfo.TYPE_NAME, fulltext);
        List<String> eps = artifacts.stream().map(NuxeoArtifact::getId).collect(Collectors.toList());
        List<ArtifactLabel> labels = eps.stream().map(ArtifactLabel::createLabelFromExtensionPoint).collect(Collectors.toList());
        return getView("listExtensionPoints").arg("eps", labels).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID)).arg("searchFilter", sanitize(fulltext));
    }

    @POST
    @Produces("text/html")
    @Path("filterContributions")
    public Object filterContributions(@FormParam("fulltext") String fulltext) {
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ExtensionInfo.TYPE_NAME, fulltext);
        return getView("listContributions").arg("contributions", artifacts).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID)).arg("searchFilter", sanitize(fulltext));
    }

    @Path("doc")
    public Resource viewDoc() {
        return ctx.newObject("documentation");
    }

    @GET
    @Produces("text/html")
    @Path("service2Bundle/{serviceId}")
    public Object service2Bundle(@PathParam("serviceId") String serviceId) {

        ServiceInfo si = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getService(serviceId);
        if (si == null) {
            return null;
        }
        String cid = si.getComponentId();

        ComponentInfo ci = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getComponent(cid);
        String bid = ci.getBundle().getId();

        org.nuxeo.common.utils.Path target = new org.nuxeo.common.utils.Path(getContext().getRoot().getName());
        target = target.append(distributionId);
        target = target.append("viewBundle");
        target = target.append(bid + "#Service." + serviceId);
        try {
            return Response.seeOther(new URI(target.toString())).build();
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }

    @GET
    @Produces("text/html")
    @Path("extensionPoint2Component/{epId}")
    public Object extensionPoint2Component(@PathParam("epId") String epId) {

        ExtensionPointInfo epi = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getExtensionPoint(
                epId);
        if (epi == null) {
            return null;
        }
        String cid = epi.getComponent().getId();

        org.nuxeo.common.utils.Path target = new org.nuxeo.common.utils.Path(getContext().getRoot().getName());
        target = target.append(distributionId);
        target = target.append("viewComponent");
        target = target.append(cid + "#extensionPoint." + epId);
        try {
            return Response.seeOther(new URI(target.toString())).build();
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }

    @Path("viewBundle/{bundleId}")
    public Resource viewBundle(@PathParam("bundleId") String bundleId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("bundle", bundleId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(bundleId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path("viewComponent/{componentId}")
    public Resource viewComponent(@PathParam("componentId") String componentId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("component", componentId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(componentId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path("viewSeamComponent/{componentId}")
    public Resource viewSeamComponent(@PathParam("componentId") String componentId) {
        return (NuxeoArtifactWebObject) ctx.newObject("seamComponent", componentId);
    }

    @Path("viewOperation/{opId}")
    public Resource viewOperation(@PathParam("opId") String opId) {
        return (NuxeoArtifactWebObject) ctx.newObject("operation", opId);
    }

    @Path("viewService/{serviceId}")
    public Resource viewService(@PathParam("serviceId") String serviceId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("service", serviceId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(serviceId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path("viewExtensionPoint/{epId}")
    public Resource viewExtensionPoint(@PathParam("epId") String epId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("extensionPoint", epId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(epId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path("viewContribution/{cId}")
    public Resource viewContribution(@PathParam("cId") String cId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("contribution", cId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(cId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path("viewBundleGroup/{gId}")
    public Resource viewBundleGroup(@PathParam("gId") String gId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("bundleGroup", gId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(gId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path("viewArtifact/{id}")
    public Object viewArtifact(@PathParam("id") String id) {
        DistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());

        BundleGroup bg = snap.getBundleGroup(id);
        if (bg != null) {
            return viewBundleGroup(id);
        }

        BundleInfo bi = snap.getBundle(id);
        if (bi != null) {
            return viewBundle(id);
        }

        ComponentInfo ci = snap.getComponent(id);
        if (ci != null) {
            return viewComponent(id);
        }

        ServiceInfo si = snap.getService(id);
        if (si != null) {
            return viewService(id);
        }

        ExtensionPointInfo epi = snap.getExtensionPoint(id);
        if (epi != null) {
            return viewExtensionPoint(id);
        }

        ExtensionInfo ei = snap.getContribution(id);
        if (ei != null) {
            return viewContribution(id);
        }

        return Response.status(404).build();
    }

    public String getLabel(String id) {
        return null;
    }

    @GET
    @Produces("text/html")
    @Path("listSeamComponents")
    public Object listSeamComponents() {
        return dolistSeamComponents("listSeamComponents", false);
    }

    @GET
    @Produces("text/html")
    @Path("listSeamComponentsSimple")
    public Object listSeamComponentsSimple() {
        return dolistSeamComponents("listSeamComponentsSimple", true);
    }

    protected Object dolistSeamComponents(String view, boolean hideNav) {

        getSnapshotManager().initSeamContext(getContext().getRequest());

        DistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        List<SeamComponentInfo> seamComponents = snap.getSeamComponents();
        return getView(view).arg("seamComponents", seamComponents).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID)).arg("hideNav", Boolean.valueOf(hideNav));
    }

    @GET
    @Produces("text/html")
    @Path("listOperations")
    public Object listOperations() {
        DistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        List<OperationInfo> operations = snap.getOperations();
        return getView("listOperations").arg("operations", operations).arg(Distribution.DIST_ID,
                ctx.getProperty(Distribution.DIST_ID)).arg("hideNav", Boolean.valueOf(false));
    }

    protected String sanitize(String value) {
        return Framework.getService(HtmlSanitizerService.class).sanitizeString(value, null);
    }

}
