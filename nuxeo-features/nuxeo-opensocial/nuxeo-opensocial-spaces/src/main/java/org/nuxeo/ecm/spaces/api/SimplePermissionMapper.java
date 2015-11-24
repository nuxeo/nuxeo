package org.nuxeo.ecm.spaces.api;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;

public interface SimplePermissionMapper {
    public Map<String, Map<String, Boolean>> getPermissions()
            throws ClientException;

    public Map<String, Boolean> getPermissions(String id)
            throws ClientException;

    public Boolean hasPermission(String id, String permission)
            throws ClientException;

}
