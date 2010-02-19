package org.nuxeo.apidoc.browse;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;

public class SimpleDocumentationItem implements DocumentationItem {

    protected List<String> applicableVersion = new ArrayList<String>();

    protected String content ="";
    protected String id = null;
    protected String renderingType="";
    protected String target="";
    protected String targetType="";
    protected String title="";
    protected String type="";
    protected String uuid="";
    protected boolean approved=false;

    public SimpleDocumentationItem() {

    }

    public SimpleDocumentationItem(NuxeoArtifact nxItem) {
        target = nxItem.getId();
        targetType = nxItem.getArtifactType();
    }

    public List<String> getApplicableVersion() {
        return applicableVersion;
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public String getRenderingType() {
        return renderingType;
    }

    public String getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return "";
    }

    public String getUUID() {
        return uuid;
    }

    public boolean isApproved() {
        return approved;
    }

}
