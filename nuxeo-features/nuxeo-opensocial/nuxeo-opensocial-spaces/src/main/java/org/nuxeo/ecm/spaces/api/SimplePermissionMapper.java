package org.nuxeo.ecm.spaces.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

public interface SimplePermissionMapper {
    public Map<String, Map<String, Boolean>> getPermissions()
    throws ClientException;

    public Map<String, Map<String, Boolean>> getPermissions(List<WebContentData> list)
            throws ClientException;

    public Map<String, Boolean> getPermissions(String id)
            throws ClientException;

    public Boolean hasPermission(String id, String permission)
            throws ClientException;

}
