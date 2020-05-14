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
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.tree.TreeHelper;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerService;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

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
            Map<String, Integer> stats = new HashMap<>();
            stats.put("bundles", snap.getBundleIds().size());
            stats.put("jComponents", snap.getJavaComponentIds().size());
            stats.put("xComponents", snap.getXmlComponentIds().size());
            stats.put("services", snap.getServiceIds().size());
            stats.put("xps", snap.getExtensionPointIds().size());
            stats.put("contribs", snap.getComponentIds().size());
            return getView("indexSimple").arg(Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID))
                                         .arg("bundleIds", snap.getBundleIds())
                                         .arg("stats", stats);
        } else {
            return getView("index").arg(Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID));
        }
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_BUNDLEGROUPS)
    public Object getMavenGroups() {
        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(
                getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()));
        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        return getView(ApiBrowserConstants.LIST_BUNDLEGROUPS).arg("tree", tree)
                                                             .arg(Distribution.DIST_ID,
                                                                     ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_BUNDLES)
    public Object getBundles() {
        List<String> bundleIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()).getBundleIds();
        return getView(ApiBrowserConstants.LIST_BUNDLES).arg("bundleIds", bundleIds)
                                                        .arg(Distribution.DIST_ID,
                                                                ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_COMPONENTS)
    public Object getComponents() {
        List<String> javaComponentIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession())
                                                            .getJavaComponentIds();
        List<ArtifactLabel> javaLabels = new ArrayList<>();
        for (String id : javaComponentIds) {
            javaLabels.add(ArtifactLabel.createLabelFromComponent(id));
        }

        List<String> xmlComponentIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession())
                                                           .getXmlComponentIds();
        List<ArtifactLabel> xmlLabels = new ArrayList<>();
        for (String id : xmlComponentIds) {
            xmlLabels.add(ArtifactLabel.createLabelFromComponent(id));
        }

        Collections.sort(javaLabels);
        Collections.sort(xmlLabels);

        return getView(ApiBrowserConstants.LIST_COMPONENTS).arg("javaComponents", javaLabels)
                                                           .arg("xmlComponents", xmlLabels)
                                                           .arg(Distribution.DIST_ID,
                                                                   ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_SERVICES)
    public Object getServices() {
        List<String> serviceIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession())
                                                      .getServiceIds();

        List<ArtifactLabel> serviceLabels = new ArrayList<>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        Collections.sort(serviceLabels);

        return getView(ApiBrowserConstants.LIST_SERVICES).arg("services", serviceLabels)
                                                         .arg(Distribution.DIST_ID,
                                                                 ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/plain")
    @Path("feedServices")
    public String feedServices() throws JSONException {
        List<String> serviceIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession())
                                                      .getServiceIds();

        List<ArtifactLabel> serviceLabels = new ArrayList<>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        Collections.sort(serviceLabels);

        JSONArray array = new JSONArray();

        for (ArtifactLabel label : serviceLabels) {
            JSONObject object = new JSONObject();
            object.put("id", label.getId());
            object.put("label", label.getLabel());
            object.put("url", "http://explorer.nuxeo.org/nuxeo/site/distribution/current/service2Bundle/" + label.id);
            array.put(object);
        }

        return array.toString();
    }

    @GET
    @Produces("text/plain")
    @Path("feedExtensionPoints")
    public String feedExtensionPoints() throws JSONException {
        List<String> epIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession())
                                                 .getExtensionPointIds();

        List<ArtifactLabel> labels = new ArrayList<>();

        for (String id : epIds) {
            labels.add(ArtifactLabel.createLabelFromExtensionPoint(id));
        }
        Collections.sort(labels);

        JSONArray array = new JSONArray();

        for (ArtifactLabel label : labels) {
            JSONObject object = new JSONObject();
            object.put("id", label.getId());
            object.put("label", label.getLabel());
            object.put("url",
                    "http://explorer.nuxeo.org/nuxeo/site/distribution/current/extensionPoint2Component/" + label.id);
            array.put(object);
        }

        return array.toString();
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_CONTRIBUTIONS)
    public Object getContributions() {
        DistributionSnapshot snapshot = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        return getView(ApiBrowserConstants.LIST_CONTRIBUTIONS).arg("contributions", snapshot.getContributions())
                                                              .arg("isLive", snapshot.isLive())
                                                              .arg(Distribution.DIST_ID,
                                                                      ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_EXTENSIONPOINTS)
    public Object getExtensionPoints() {
        List<String> epIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession())
                                                 .getExtensionPointIds();

        List<ArtifactLabel> labels = epIds.stream()
                                          .map(ArtifactLabel::createLabelFromExtensionPoint)
                                          .collect(Collectors.toList());

        Collections.sort(labels);
        return getView(ApiBrowserConstants.LIST_EXTENSIONPOINTS).arg("eps", labels)
                                                                .arg(Distribution.DIST_ID,
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
        return getView(ApiBrowserConstants.LIST_COMPONENTS).arg("javaComponents", javaLabels)
                                                           .arg("xmlComponents", xmlLabels)
                                                           .arg(Distribution.DIST_ID,
                                                                   ctx.getProperty(Distribution.DIST_ID))
                                                           .arg("searchFilter", sanitize(fulltext));
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
        List<String> bundleIds = new ArrayList<>();
        for (NuxeoArtifact item : artifacts) {
            bundleIds.add(item.getId());
        }
        return getView(ApiBrowserConstants.LIST_BUNDLES).arg("bundleIds", bundleIds)
                                                        .arg(Distribution.DIST_ID,
                                                                ctx.getProperty(Distribution.DIST_ID))
                                                        .arg("searchFilter", sanitize(fulltext));
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
        List<String> serviceIds = new ArrayList<>();
        for (NuxeoArtifact item : artifacts) {
            serviceIds.add(item.getId());
        }
        List<ArtifactLabel> serviceLabels = new ArrayList<>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        return getView(ApiBrowserConstants.LIST_SERVICES).arg("services", serviceLabels)
                                                         .arg(Distribution.DIST_ID,
                                                                 ctx.getProperty(Distribution.DIST_ID))
                                                         .arg("searchFilter", sanitize(fulltext));
    }

    @POST
    @Produces("text/html")
    @Path("filterExtensionPoints")
    public Object filterExtensionPoints(@FormParam("fulltext") String fulltext) {
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ExtensionPointInfo.TYPE_NAME, fulltext);
        List<String> eps = artifacts.stream().map(NuxeoArtifact::getId).collect(Collectors.toList());
        List<ArtifactLabel> labels = eps.stream()
                                        .map(ArtifactLabel::createLabelFromExtensionPoint)
                                        .collect(Collectors.toList());
        return getView(ApiBrowserConstants.LIST_EXTENSIONPOINTS).arg("eps", labels)
                                                                .arg(Distribution.DIST_ID,
                                                                        ctx.getProperty(Distribution.DIST_ID))
                                                                .arg("searchFilter", sanitize(fulltext));
    }

    @POST
    @Produces("text/html")
    @Path("filterContributions")
    public Object filterContributions(@FormParam("fulltext") String fulltext) {
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ExtensionInfo.TYPE_NAME, fulltext);
        return getView(ApiBrowserConstants.LIST_CONTRIBUTIONS).arg("contributions", artifacts)
                                                              .arg(Distribution.DIST_ID,
                                                                      ctx.getProperty(Distribution.DIST_ID))
                                                              .arg("searchFilter", sanitize(fulltext));
    }

    @Path("doc")
    public Resource viewDoc() {
        return ctx.newObject(ApiBrowserConstants.VIEW_DOCUMENTATION);
    }

    /**
     * Handles navigation to plugin view.
     *
     * @since 11.1
     */
    @Path("{pluginId}")
    public Object plugin(@PathParam("pluginId") String pluginId) {
        return ctx.newObject(pluginId, distributionId, embeddedMode);
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
        target = target.append(ApiBrowserConstants.VIEW_BUNDLE);
        target = target.append(bid).append("#Service.").append(serviceId);
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

        ExtensionPointInfo epi = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession())
                                                     .getExtensionPoint(epId);
        if (epi == null) {
            return null;
        }
        String cid = epi.getComponent().getId();

        org.nuxeo.common.utils.Path target = new org.nuxeo.common.utils.Path(getContext().getRoot().getName());
        target = target.append(distributionId);
        target = target.append(ApiBrowserConstants.VIEW_COMPONENT);
        target = target.append(cid).append("#extensonPoint.").append(epId);
        try {
            return Response.seeOther(new URI(target.toString())).build();
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }

    @Path(ApiBrowserConstants.VIEW_BUNDLE + "/{bundleId}")
    public Resource viewBundle(@PathParam("bundleId") String bundleId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("bundle", bundleId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(bundleId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_COMPONENT + "/{componentId}")
    public Resource viewComponent(@PathParam("componentId") String componentId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("component", componentId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(componentId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_OPERATION + "/{opId}")
    public Resource viewOperation(@PathParam("opId") String opId) {
        return ctx.newObject("operation", opId);
    }

    @Path(ApiBrowserConstants.VIEW_SERVICE + "/{serviceId}")
    public Resource viewService(@PathParam("serviceId") String serviceId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("service", serviceId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(serviceId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_EXTENSIONPOINT + "/{epId}")
    public Resource viewExtensionPoint(@PathParam("epId") String epId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("extensionPoint", epId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(epId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_CONTRIBUTION + "/{cId}")
    public Resource viewContribution(@PathParam("cId") String cId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("contribution", cId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(cId);
        }
        TreeHelper.updateTree(getContext(), nxItem.getHierarchyPath());
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_BUNDLEGROUP + "/{gId}")
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
    @Path(ApiBrowserConstants.LIST_OPERATIONS)
    public Object listOperations() {
        DistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        List<OperationInfo> operations = snap.getOperations();
        return getView(ApiBrowserConstants.LIST_OPERATIONS).arg("operations", operations)
                                                           .arg(Distribution.DIST_ID,
                                                                   ctx.getProperty(Distribution.DIST_ID))
                                                           .arg("hideNav", Boolean.valueOf(false));
    }

    protected String sanitize(String value) {
        return Framework.getService(HtmlSanitizerService.class).sanitizeString(value, null);
    }

}
