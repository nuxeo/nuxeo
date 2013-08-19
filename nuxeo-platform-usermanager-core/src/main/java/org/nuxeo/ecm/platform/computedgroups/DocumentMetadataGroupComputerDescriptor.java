package org.nuxeo.ecm.platform.computedgroups;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.computedgroups.GroupComputer;
import org.nuxeo.ecm.platform.computedgroups.GroupComputerDescriptor;

@XObject("documentMetadataGroupComuter")
/**
 * @since 5.7.3
 */
public class DocumentMetadataGroupComputerDescriptor extends
        GroupComputerDescriptor {

    private static final long serialVersionUID = 1L;

    @XNode("@whereClause")
    public String whereClause = "";

    @XNode("@groupPattern")
    public String groupPattern = "%s";

    @XNode("@xpath")
    public String xpathSelector = "ecm:uuid";

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
        return new DocumentMetadataGroupComputer(whereClause,
                groupPattern, xpathSelector);
    };

}
