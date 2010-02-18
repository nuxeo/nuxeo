package org.nuxeo.apidoc.api;

import java.util.List;

public interface DocumentationItem {

    String getTitle();

    String getContent();

    String getType();

    String getRenderingType();

    String getTypeLabel();

    List<String> getApplicableVersion();

    String getTarget();

    String getTargetType();

    boolean isApproved();

    String getId();

    String getUUID();

}
