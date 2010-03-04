package org.nuxeo.opensocial.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.shindig.config.ContainerConfig;

public class FakeContainerConfig implements ContainerConfig {

    public boolean getBool(String container, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    public Collection<String> getContainers() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getInt(String container, String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    public List<Object> getList(String container, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Object> getMap(String container, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Object> getProperties(String container) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getProperty(String container, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getString(String container, String name) {
        return "insecure";
    }

}
