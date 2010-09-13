package org.nuxeo.ecm.core.management.statuses;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.management.api.AdministrativeStatus;

@XObject("deactivableService")
public class AdministrableServiceDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@label")
    private String label;

    @XNode("@id")
    private String id;

    @XNode("description")
    private String description;

    @XNode("initialState")
    private String initialState = AdministrativeStatus.ACTIVE;

    public String getInitialState() {
        return initialState;
    }

    public String getLabel() {
        return label;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getUniqueId() {
        return getId();
    }



}
