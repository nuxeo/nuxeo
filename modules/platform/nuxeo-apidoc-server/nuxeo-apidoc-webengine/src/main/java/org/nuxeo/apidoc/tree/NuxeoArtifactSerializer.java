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
package org.nuxeo.apidoc.tree;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.ecm.webengine.ui.tree.TreeItem;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class NuxeoArtifactSerializer extends JSonTreeSerializer {

    protected final WebContext ctx;

    protected final DistributionSnapshot ds;

    public NuxeoArtifactSerializer(WebContext ctx, DistributionSnapshot ds) {
        this.ctx = ctx;
        this.ds = ds;
    }

    protected static boolean useEmbeddedMode(WebContext ctx) {
        return ((Boolean) ctx.getProperty(ApiBrowserConstants.EMBEDDED_MODE_MARKER, Boolean.FALSE)).booleanValue();
    }

    @Override
    public String getUrl(TreeItem item) {

        NuxeoArtifact obj = (NuxeoArtifact) item.getObject();

        String distId = ds.getKey().replace(" ", "%20");
        if (ds.isLive()) {
            if (useEmbeddedMode(ctx)) {
                distId = SnapshotManager.DISTRIBUTION_ALIAS_ADM;
            } else {
                distId = SnapshotManager.DISTRIBUTION_ALIAS_CURRENT;
            }
        }
        String url = ctx.getRoot().getURL() + "/" + distId + "/";

        if (obj.getArtifactType().equals(DistributionSnapshot.TYPE_NAME)) {
            return url;
        }
        String view = ApiBrowserConstants.getArtifactView(obj.getArtifactType());
        if (view != null) {
            url += view + "/" + obj.getId();
        } else {
            if (obj instanceof VirtualNode) {
                VirtualNode vn = (VirtualNode) obj;
                url = String.format("%s/%s/%s/%s#%s", ctx.getRoot().getURL(), distId,
                        ApiBrowserConstants.VIEW_COMPONENT, vn.getComponentId(), vn.getAnchor());
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
        json.element("text", item.getLabel())
            .element("id", item.getPath().toString())
            .element("href", getUrl(item))
            .element("classes", classes)
            .element("class", classes);
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
