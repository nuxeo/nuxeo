package org.nuxeo.opensocial.container.client.external;

import java.util.Map;

public interface HasPermissions {
    public Map<String, Boolean> getPermissions();

    public Boolean hasPermission(String permission);
}
