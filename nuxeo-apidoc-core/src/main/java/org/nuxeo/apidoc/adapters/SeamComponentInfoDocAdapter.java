package org.nuxeo.apidoc.adapters;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

public class SeamComponentInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements SeamComponentInfo {

    protected SeamComponentInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    @Override
    public String getId() {
        return "seam:"+ getName();
    }

    public String getClassName() {
        return safeGet("nxseam:className");
    }

    @SuppressWarnings("unchecked")
    public List<String> getInterfaceNames() {
        try {
            return (List<String>) doc.getPropertyValue("nxseam:interfaces");
        }
        catch (Exception e) {
            log.error("Error while getting service names", e);
        }
        return null;
    }

    public String getName() {
        return safeGet("nxseam:componentName");
    }

    public String getPrecedence() {
        return safeGet("nxseam:precedence");
    }

    public String getScope() {
        return safeGet("nxseam:scope");
    }

    public String getArtifactType() {
        return SeamComponentInfo.TYPE_NAME;
    }

    public String getVersion() {
        DistributionSnapshot parentSnapshot = getParentNuxeoArtifact(DistributionSnapshot.class);

        if (parentSnapshot == null) {
            log.error("Unable to determine version for bundleGroup " + getId());
            return "?";
        }

        return parentSnapshot.getVersion();
    }

    @Override
    public int compareTo(SeamComponentInfo o) {
        return getClassName().compareTo(o.getClassName());
    }


    public static SeamComponentInfo create(SeamComponentInfo sci, CoreSession session, String containerPath) throws Exception {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName(sci.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }

        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", sci.getName());

        doc.setPropertyValue("nxseam:componentName", sci.getName());
        doc.setPropertyValue("nxseam:className", sci.getClassName());
        doc.setPropertyValue("nxseam:scope", sci.getScope());
        doc.setPropertyValue("nxseam:interfaces", (Serializable) sci.getInterfaceNames());

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }

        return new SeamComponentInfoDocAdapter(doc);
    }

}
