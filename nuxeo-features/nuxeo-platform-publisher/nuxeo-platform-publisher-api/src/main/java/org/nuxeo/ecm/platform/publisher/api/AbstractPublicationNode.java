package org.nuxeo.ecm.platform.publisher.api;

public abstract class AbstractPublicationNode implements PublicationNode {

    protected String treeName;

    public String getNodeType() {
        return getType();
    }

    public String getType() {
        return this.getClass().getSimpleName();
    }

    public String getTreeConfigName() {
        return treeName;
    }

}
