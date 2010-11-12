package org.nuxeo.apidoc.api;

import java.util.List;

public interface SeamComponentInfo extends NuxeoArtifact, Comparable<SeamComponentInfo> {

    String TYPE_NAME ="NXSeamComponent";

    public String getName();

    public String getScope();

    public String getPrecedence();

    public String getClassName();

    public List<String> getInterfaceNames();

}
