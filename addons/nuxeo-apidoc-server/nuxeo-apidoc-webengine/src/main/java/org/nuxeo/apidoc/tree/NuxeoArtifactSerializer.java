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
package org.nuxeo.apidoc.tree;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.ecm.webengine.ui.tree.TreeItem;

public class NuxeoArtifactSerializer extends JSonTreeSerializer {

    protected final WebContext ctx;

    protected final DistributionSnapshot ds;

    public NuxeoArtifactSerializer(WebContext ctx, DistributionSnapshot ds) {
        this.ctx = ctx;
        this.ds = ds;
    }

    protected static boolean useEmbeddedMode(WebContext ctx) {
        return ((Boolean) ctx.getProperty("embeddedMode", Boolean.FALSE)).booleanValue();
    }

    @Override
    public String getUrl(TreeItem item) {

        NuxeoArtifact obj = (NuxeoArtifact) item.getObject();

        String distId = ds.getKey().replace(" ", "%20");
        if (ds.isLive()) {
            if (useEmbeddedMode(ctx)) {
                distId = "adm";
            } else {
                distId = "current";
            }
        }
        String url = ctx.getRoot().getURL() + "/" + distId + "/";

        if (obj.getArtifactType().equals(DistributionSnapshot.TYPE_NAME)) {
            return url;
        } else if (obj.getArtifactType().equals(BundleInfo.TYPE_NAME)) {
            url += "viewBundle/";
        } else if (obj.getArtifactType().equals(BundleGroup.TYPE_NAME)) {
            url += "viewBundleGroup/";
        } else if (obj.getArtifactType().equals(ComponentInfo.TYPE_NAME)) {
            url += "viewComponent/";
        } else if (obj.getArtifactType().equals(ExtensionInfo.TYPE_NAME)) {
            url += "viewContribution/";
        } else if (obj.getArtifactType().equals(ExtensionPointInfo.TYPE_NAME)) {
            url += "viewExtensionPoint/";
        } else if (obj.getArtifactType().equals(ServiceInfo.TYPE_NAME)) {
            url += "viewService/";
        } else {
            url = null;
        }

        if (url != null) {
            url += obj.getId();
        } else {
            if (obj instanceof VirtualNode) {
                VirtualNode vn = (VirtualNode) obj;
                url = ctx.getRoot().getURL() + "/" + distId + "/viewComponent/"
                        + vn.getComponentId() + "#" + vn.getAnchor();
            } else {
                url = "todo";
            }
        }
        return url;
    }

    @Override
    protected JSONObject item2JSON(TreeItem item, JSONArray children) {
        JSONObject json = new JSONObject();
        String[] classes = { "folder", "Folder" };
        json.element("text", item.getLabel()).element("id",
                item.getPath().toString()).element("href", getUrl(item)).element(
                "classes", classes).element("class", classes);
        json.element("expanded", item.isExpanded());
        if (item.isContainer()) {
            if (item.isContainer()) {
                if (item.hasChildren()) {
                    json.element("children", children);
                } else {
                    json.element("hasChildren", true);
                }
            } else {
                json.element("hasChildren", false);
            }
        }
        return json;
    }
}
