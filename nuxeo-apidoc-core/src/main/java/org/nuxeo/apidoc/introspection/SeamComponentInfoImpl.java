package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.SeamComponentInfo;

public class SeamComponentInfoImpl extends BaseNuxeoArtifact implements SeamComponentInfo {

    protected String name;

    protected String scope;

    protected String precedence;

    protected String className;

    protected List<String> interfaceNames = new ArrayList<String>();

    protected String version;

    public String getName() {
        return name;
    }

    public String getScope() {
        return scope;
    }

    public String getPrecedence() {
        return precedence;
    }

    public String getClassName() {
        return className;
    }

    public int compareTo(SeamComponentInfoImpl o) {
        return className.compareTo(o.className);
    }

    public void addInterfaceName(String name) {
        if (!interfaceNames.contains(name)) {
            interfaceNames.add(name);
        }
    }

    public List<String> getInterfaceNames() {
        return interfaceNames;
    }

    @Override
    public String getArtifactType() {
        return SeamComponentInfo.TYPE_NAME;
    }

    @Override
    public String getId() {
        return "seam:"+ getName();
    }

    @Override
    public String getHierarchyPath() {
        return "/";
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version=version;
    }

    @Override
    public int compareTo(SeamComponentInfo o) {
        return getClassName().compareTo(o.getClassName());
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setPrecedence(String precedence) {
        this.precedence = precedence;
    }

    public void setClassName(String className) {
        this.className = className;
    }

}
