package org.nuxeo.opensocial.container.client.external.html;

import java.util.Map;

import org.nuxeo.opensocial.container.client.external.HasPermissions;
import org.nuxeo.opensocial.container.shared.webcontent.HTMLData;

/**
 * @author Stéphane Fourrier
 */
public class HTMLModel implements HasPermissions {
    private HTMLData data;

    private Map<String, Boolean> permissions;

    public HTMLModel(HTMLData data, Map<String, Boolean> permissions) {
        this.permissions = permissions;
        this.data = data;
    }

    public HTMLData getData() {
        return data;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public Boolean hasPermission(String permission) {
        return permissions.containsKey(permission);
    }
}
