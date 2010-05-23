package org.nuxeo.apidoc.tree;

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.ecm.core.api.CoreSession;

public class VirtualNode implements NuxeoArtifact {

    protected String cid;
    protected String version;
    protected final String type;
    protected String id;
    protected String basePath;

    public VirtualNode(ComponentInfo ci, String type, String id) {
        cid=ci.getId();
        version = ci.getVersion();
        this.type=type;
        this.id=id;
        basePath = ci.getHierarchyPath();
    }

    public String getComponentId() {
        return cid;
    }

    public String getArtifactType() {
        return type;
    }

    public AssociatedDocuments getAssociatedDocuments(CoreSession session) {
        return null;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getHierarchyPath() {
        if (ServiceInfo.TYPE_NAME.equals(type)) {
            return basePath + "/" + VirtualNodesConsts.Services_VNODE ;
        }
        else if (ExtensionInfo.TYPE_NAME.equals(type)) {
            return basePath + "/" + VirtualNodesConsts.Contributions_VNODE ;
        }
        else if (ExtensionPointInfo.TYPE_NAME.equals(type)) {
            return basePath + "/" + VirtualNodesConsts.ExtensionPoints_VNODE ;
        }
        return "";
    }
}
