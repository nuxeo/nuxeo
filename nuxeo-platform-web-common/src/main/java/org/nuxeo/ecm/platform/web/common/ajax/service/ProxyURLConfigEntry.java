package org.nuxeo.ecm.platform.web.common.ajax.service;

/**
 *
 *
 * @author tiry
 *
 */
public class ProxyURLConfigEntry {

    protected boolean granted = false;

    protected String descriptorName;

    protected boolean useCache;

    protected boolean cachePerSession;


    public ProxyURLConfigEntry() {
        this.granted=false;
    }

    public ProxyURLConfigEntry( boolean granted, ProxyableURLDescriptor desc) {
        this.granted=granted;
        this.descriptorName=desc.getName();
        useCache = desc.useCache;
        cachePerSession = desc.cachePerSession;
    }

    public boolean isGranted() {
        return granted;
    }

    public String getDescriptorName() {
        return descriptorName;
    }

    public boolean useCache() {
        return useCache;
    }

    public boolean isCachePerSession() {
        if (!useCache) {
            return false;
        }
        return cachePerSession;
    }




}
