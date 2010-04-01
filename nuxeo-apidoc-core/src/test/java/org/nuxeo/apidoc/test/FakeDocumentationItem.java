package org.nuxeo.apidoc.test;

import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.DocumentationItem;

public class FakeDocumentationItem implements DocumentationItem {

    public List<String> applicableVersion;
    public String content;
    public String id;
    public String renderingType;
    public String target;
    public String targetType;
    public String title;
    public String type;
    public String typeLabel;
    public String uuid;
    public boolean approved=false;

    public FakeDocumentationItem() {

    }

    public FakeDocumentationItem(DocumentationItem item) {
        applicableVersion = item.getApplicableVersion();
        content = item.getContent();
        id = item.getId();
        renderingType = item.getRenderingType();
        target = item.getTarget();
        title = item.getTitle();
        type = item.getType();
        typeLabel = item.getTypeLabel();
        uuid = item.getUUID();
        approved = item.isApproved();
        type = item.getTargetType();
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

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public String getUUID() {
        return uuid;
    }

    public boolean isApproved() {
        return approved;
    }

    public String getTargetType() {
        return targetType;
    }

    public Map<String, String> getAttachements() {
        // TODO Auto-generated method stub
        return null;
    }

}
