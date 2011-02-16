package org.nuxeo.opensocial.container.client.external.picture;

import java.util.Map;

import org.nuxeo.opensocial.container.client.external.HasPermissions;
import org.nuxeo.opensocial.container.shared.webcontent.PictureData;

/**
 * @author Stéphane Fourrier
 */
public class PictureModel implements HasPermissions {
    private PictureData data;

    private Map<String, Boolean> permissions;

    public PictureModel(PictureData data, Map<String, Boolean> permissions  ) {
        this.data = data;
        this.permissions = permissions;
    }

    public PictureData getData() {
        return data;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public Boolean hasPermission(String permission) {
        return permissions.containsKey(permission);
    }
}
