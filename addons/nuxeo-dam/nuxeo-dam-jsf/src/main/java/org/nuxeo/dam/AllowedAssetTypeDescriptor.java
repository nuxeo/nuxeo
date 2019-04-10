package org.nuxeo.dam;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor object allowed to register asset type that can be created in DAM.
 *
 * @since 5.7
 */
@XObject("allowedAssetType")
public class AllowedAssetTypeDescriptor {

    @XNode("@name")
    String name;

    @XNode("@enabled")
    boolean enabled = true;

    public AllowedAssetTypeDescriptor() {
    }

    public AllowedAssetTypeDescriptor(AllowedAssetTypeDescriptor other) {
        name = other.name;
        enabled = other.enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
