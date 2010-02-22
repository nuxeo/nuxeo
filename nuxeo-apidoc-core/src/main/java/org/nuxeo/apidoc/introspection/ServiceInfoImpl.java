package org.nuxeo.apidoc.introspection;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;

public class ServiceInfoImpl extends BaseNuxeoArtifact implements ServiceInfo {

    protected String serviceClassName;

    protected ComponentInfo component;

    public ServiceInfoImpl(String serviceClassName, ComponentInfo component) {
        this.serviceClassName = serviceClassName;
        this.component=component;
    }

    @Override
    public String getId() {
        return serviceClassName;
    }

    public String getArtifactType() {
        return ServiceInfo.TYPE_NAME;
    }

    public String getVersion() {
        return component.getVersion();
    }

}
