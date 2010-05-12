/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
@WebObject(type = "apibrowser")
public class ApiBrowser extends DefaultObject {

    String distributionId = null;

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }

    protected ArtifactSearcher getSearcher() {
        return Framework.getLocalService(ArtifactSearcher.class);
    }

    @Override
    protected void initialize(Object... args) {
        distributionId = (String) args[0];
    }


    @GET
    @Produces("text/html")
    public Object doGet() {
        return getView("index").arg("distId", ctx.getProperty("distId"));
    }

    @GET
    @Produces("text/html")
    @Path(value = "listBundleGroups")
    public Object getMavenGroups() {
        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(getSnapshotManager().getSnapshot(distributionId,ctx.getCoreSession()));
        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        return getView("listBundleGroups").arg("tree", tree).arg("distId", ctx.getProperty("distId"));
    }

    @GET
    @Produces("text/html")
    @Path(value = "listBundles")
    public Object getBundles() {
        List<String> bundleIds = getSnapshotManager().getSnapshot(distributionId,ctx.getCoreSession()).getBundleIds();
        return getView("listBundles").arg("bundleIds", bundleIds).arg("distId", ctx.getProperty("distId"));
    }

    @GET
    @Produces("text/html")
    @Path(value = "filterBundles")
    public Object filterBundles() throws Exception {
        String fulltext = getContext().getForm().getFormProperty("fulltext");
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId, BundleInfo.TYPE_NAME, fulltext);
        List<String> bundleIds = new ArrayList<String>();
        for (NuxeoArtifact item : artifacts) {
            bundleIds.add(item.getId());
        }
        return getView("listBundles").arg("bundleIds", bundleIds).arg("distId", ctx.getProperty("distId")).arg("searchFilter", fulltext);
    }

    @GET
    @Produces("text/html")
    @Path(value = "listComponents")
    public Object getComponents() {
        List<String> javaComponentIds = getSnapshotManager().getSnapshot(distributionId,ctx.getCoreSession()).getJavaComponentIds();
        List<ArtifactLabel> javaLabels = new ArrayList<ArtifactLabel>();
        for (String id : javaComponentIds) {
            javaLabels.add(ArtifactLabel.createLabelFromComponent(id));
        }

        List<String> xmlComponentIds = getSnapshotManager().getSnapshot(distributionId,ctx.getCoreSession()).getXmlComponentIds();
        List<ArtifactLabel> xmlLabels = new ArrayList<ArtifactLabel>();
        for (String id : xmlComponentIds) {
            xmlLabels.add(ArtifactLabel.createLabelFromComponent(id));
        }

        Collections.sort(javaLabels);
        Collections.sort(xmlLabels);

        return getView("listComponents").arg("javaComponents", javaLabels).arg("xmlComponents", xmlLabels).arg("distId", ctx.getProperty("distId"));
    }

    @GET
    @Produces("text/html")
    @Path(value = "filterComponents")
    public Object filterComponents() throws Exception {
        String fulltext = getContext().getForm().getFormProperty("fulltext");
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId, ComponentInfo.TYPE_NAME, fulltext);

        List<ArtifactLabel> xmlLabels = new ArrayList<ArtifactLabel>();
        List<ArtifactLabel> javaLabels = new ArrayList<ArtifactLabel>();

        for (NuxeoArtifact item : artifacts) {
            ComponentInfo ci = (ComponentInfo) item;
            if (ci.isXmlPureComponent()) {
                xmlLabels.add(ArtifactLabel.createLabelFromComponent(ci.getId()));
            } else {
                javaLabels.add(ArtifactLabel.createLabelFromComponent(ci.getId()));
            }
        }
        return getView("listComponents").arg("javaComponents", javaLabels).arg("xmlComponents", xmlLabels).arg("distId", ctx.getProperty("distId")).arg("searchFilter", fulltext);
    }


    @GET
    @Produces("text/html")
    @Path(value = "listServices")
    public Object getServices() {
        List<String> serviceIds = getSnapshotManager().getSnapshot(distributionId,ctx.getCoreSession()).getServiceIds();

        List<ArtifactLabel> serviceLabels = new ArrayList<ArtifactLabel>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        Collections.sort(serviceLabels);

        return getView("listServices").arg("services", serviceLabels).arg("distId", ctx.getProperty("distId"));
    }

    @GET
    @Produces("text/html")
    @Path(value = "filterServices")
    public Object filterServices() throws Exception {
        String fulltext = getContext().getForm().getFormProperty("fulltext");
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId, ServiceInfo.TYPE_NAME, fulltext);
        List<String> serviceIds = new ArrayList<String>();
        for (NuxeoArtifact item : artifacts) {
            serviceIds.add(item.getId());
        }
        List<ArtifactLabel> serviceLabels = new ArrayList<ArtifactLabel>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        return getView("listServices").arg("services", serviceLabels).arg("distId", ctx.getProperty("distId")).arg("searchFilter", fulltext);
    }

    @GET
    @Produces("text/html")
    @Path(value = "listExtensionPoints")
    public Object getExtensionPoints() {
        List<String> epIds = getSnapshotManager().getSnapshot(distributionId,ctx.getCoreSession()).getExtensionPointIds();

        List<ArtifactLabel> labels = new ArrayList<ArtifactLabel>();
        for (String id : epIds) {
            labels.add(ArtifactLabel.createLabelFromExtensionPoint(id));
        }

        Collections.sort(labels);
        return getView("listExtensionPoints").arg("eps", labels).arg("distId", ctx.getProperty("distId"));
    }

    @GET
    @Produces("text/html")
    @Path(value = "filterExtensionPoints")
    public Object filterExtensionPoints() throws Exception {
        String fulltext = getContext().getForm().getFormProperty("fulltext");
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId, ExtensionPointInfo.TYPE_NAME, fulltext);
        List<String> eps = new ArrayList<String>();
        for (NuxeoArtifact item : artifacts) {
            eps.add(item.getId());
        }
        List<ArtifactLabel> labels = new ArrayList<ArtifactLabel>();
        for (String id : eps) {
            labels.add(ArtifactLabel.createLabelFromExtensionPoint(id));
        }
        return getView("listExtensionPoints").arg("eps",labels).arg("distId", ctx.getProperty("distId")).arg("searchFilter", fulltext);
    }


    @GET
    @Produces("text/html")
    @Path(value = "listContributions")
    public Object getContributions() {
        List<String> cIds = getSnapshotManager().getSnapshot(distributionId,ctx.getCoreSession()).getContributionIds();
        return getView("listContributions").arg("cIds", cIds).arg("distId", ctx.getProperty("distId"));
    }

    @GET
    @Produces("text/html")
    @Path(value = "filterContributions")
    public Object filterContributions() throws Exception {
        String fulltext = getContext().getForm().getFormProperty("fulltext");
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId, ExtensionPointInfo.TYPE_NAME, fulltext);
        List<String> cIds = new ArrayList<String>();
        for (NuxeoArtifact item : artifacts) {
            cIds.add(item.getId());
        }
        return getView("listContributions").arg("cIds", cIds).arg("distId", ctx.getProperty("distId")).arg("searchFilter", fulltext);
    }


    @Path(value = "doc")
    public Resource viewDoc() {
        try {
            return ctx.newObject("documentation");
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }


    @Path(value = "viewBundle/{bundleId}")
    public Resource viewBundle(@PathParam("bundleId") String bundleId) {
        try {
            return ctx.newObject("bundle", bundleId);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @Path(value = "viewComponent/{componentId}")
    public Resource viewComponent(@PathParam("componentId") String componentId) {
        try {
            return ctx.newObject("component", componentId);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @Path(value = "viewService/{serviceId}")
    public Resource viewService(@PathParam("serviceId") String serviceId) {
        try {
            return ctx.newObject("service", serviceId);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @Path(value = "viewExtensionPoint/{epId}")
    public Resource viewExtensionPoint(@PathParam("epId") String epId) {
        try {
            return ctx.newObject("extensionPoint", epId);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @Path(value = "viewContribution/{cId}")
    public Resource viewContribution(@PathParam("cId") String cId) {
        try {
            return ctx.newObject("contribution", cId);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @Path(value = "viewBundleGroup/{gId}")
    public Resource viewBundleGroup(@PathParam("gId") String gId) {
        try {
            return ctx.newObject("bundleGroup", gId);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    public String getLabel(String id) {
        return null;
    }

}
