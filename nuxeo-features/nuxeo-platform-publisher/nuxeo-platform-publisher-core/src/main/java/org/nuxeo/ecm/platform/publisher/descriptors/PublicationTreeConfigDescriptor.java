package org.nuxeo.ecm.platform.publisher.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Descriptor for a PublicationTree configuration
 *
 * @author tiry
 */
@XObject("publicationTreeConfig")
public class PublicationTreeConfigDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    private String name;

    @XNode("@tree")
    private String tree;

    @XNode("@title")
    private String title;

    @XNode("@validatorsRule")
    private String validatorsRule;

    @XNode("@factory")
    private String factory = null;

    @XNode("@localSectionTree")
    private boolean localSectionTree = false;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> parameters = new HashMap<String, String>();

    public PublicationTreeConfigDescriptor() {
    }

    public PublicationTreeConfigDescriptor(PublicationTreeConfigDescriptor other) {
        name = other.name;
        tree = other.tree;
        title = other.title;
        validatorsRule = other.validatorsRule;
        factory = other.factory;
        localSectionTree = other.localSectionTree;
        parameters = new HashMap<String, String>(other.parameters);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTree() {
        return tree;
    }

    public void setTree(String tree) {
        this.tree = tree;
    }

    public String getValidatorsRule() {
        return validatorsRule;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public boolean islocalSectionTree() {
        return localSectionTree;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getTitle() {
        return title;
    }

}
