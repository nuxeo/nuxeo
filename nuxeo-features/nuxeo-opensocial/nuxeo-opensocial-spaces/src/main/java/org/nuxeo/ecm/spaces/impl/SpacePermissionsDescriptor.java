package org.nuxeo.ecm.spaces.impl;

import java.util.List;

import org.jsecurity.util.CollectionUtils;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("spacePermissions")
public class SpacePermissionsDescriptor {

    @XNodeList(value = "permission", type = String[].class, componentType = String.class)
    public String[] permissions;

    /**
     * @return the permissions
     */
    public List<String> getPermissions() {
        return CollectionUtils.asList(permissions);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SpacePermissionsDescriptor [permissions=");
        if (permissions.length > 0) {
            for (String entry : permissions) {
                sb.append(entry + ", ");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        return sb.toString();
    }

}
