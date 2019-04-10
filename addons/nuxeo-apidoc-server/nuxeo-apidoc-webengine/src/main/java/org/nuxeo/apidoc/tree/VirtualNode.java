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

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.ecm.core.api.CoreSession;

public class VirtualNode implements NuxeoArtifact {

    protected final String cid;

    protected final String version;

    protected final String type;

    protected final String id;

    protected final String basePath;

    public VirtualNode(ComponentInfo ci, String type, String id) {
        cid = ci.getId();
        version = ci.getVersion();
        this.type = type;
        this.id = id;
        basePath = ci.getHierarchyPath();
    }

    public String getComponentId() {
        return cid;
    }

    @Override
    public String getArtifactType() {
        return type;
    }

    @Override
    public AssociatedDocuments getAssociatedDocuments(CoreSession session) {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getHierarchyPath() {
        if (VirtualNodesConsts.Services_VNODE.equals(type)) {
            return basePath + "/" + VirtualNodesConsts.Services_VNODE_NAME;
        } else if (VirtualNodesConsts.Contributions_VNODE.equals(type)) {
            return basePath + "/" + VirtualNodesConsts.Contributions_VNODE_NAME;
        } else if (VirtualNodesConsts.ExtensionPoints_VNODE.equals(type)) {
            return basePath + "/" + VirtualNodesConsts.ExtensionPoints_VNODE_NAME;
        }
        return "";
    }

    public String getAnchor() {
        if (VirtualNodesConsts.Services_VNODE.equals(type)) {
            return "services";
        } else if (VirtualNodesConsts.ExtensionPoints_VNODE.equals(type)) {
            return "extensionPoints";
        } else if (VirtualNodesConsts.Contributions_VNODE.equals(type)) {
            return "contributions";
        }
        return "";
    }
}
