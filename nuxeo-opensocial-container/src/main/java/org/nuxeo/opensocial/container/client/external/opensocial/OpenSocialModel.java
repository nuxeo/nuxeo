package org.nuxeo.opensocial.container.client.external.opensocial;

import java.util.Map;

import org.nuxeo.opensocial.container.client.external.HasPermissions;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;

/**
 * @author Stéphane Fourrier
 */
public class OpenSocialModel implements HasPermissions {
    private OpenSocialData data;

    private Map<String, Boolean> permissions;

    public OpenSocialModel(OpenSocialData data, Map<String, Boolean> permissions) {
        this.data = data;
        this.permissions = permissions;
    }

    public OpenSocialData getData() {
        return data;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public Boolean hasPermission(String permission) {
        return permissions.containsKey(permission);
    }

}
