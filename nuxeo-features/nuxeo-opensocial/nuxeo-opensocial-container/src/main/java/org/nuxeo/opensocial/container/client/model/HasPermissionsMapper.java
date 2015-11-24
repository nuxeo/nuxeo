package org.nuxeo.opensocial.container.client.model;

import java.util.Map;

/**
 * @author Stéphane Fourrier
 */
public interface HasPermissionsMapper {
    public Map<String, Map<String, Boolean>> getPermissions();

    public Boolean hasPermission(String id, String permission);
}
