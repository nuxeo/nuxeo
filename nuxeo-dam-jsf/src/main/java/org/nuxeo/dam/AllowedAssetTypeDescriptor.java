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

    @Override
    public AllowedAssetTypeDescriptor clone() {
        AllowedAssetTypeDescriptor clone = new AllowedAssetTypeDescriptor();
        clone.setName(getName());
        clone.setEnabled(isEnabled());
        return clone;
    }
}
