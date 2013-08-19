package org.nuxeo.ecm.platform.computedgroups;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;

@XObject("userMetadataGroupComputer")
/**
 * @since 5.7.3
 */
public class UserMetadataGroupComputerDescriptor extends GroupComputerDescriptor {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XNode("@xpath")
    public String xpath;

    @XNode("@groupPattern")
    public String groupPattern = "%s";

    @XNode("@name")
    public String name;

    @XNode("@enabled")
    public boolean enabled = true;

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        return computerClass.getSimpleName();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

   @Override
    public GroupComputer getComputer() throws ClientException {
        return new UserMetadataGroupComputer(xpath, groupPattern);
    }

}
