package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.spaces.api.AbstractUnivers;
import org.nuxeo.ecm.spaces.api.Univers;

public class VirtualUnivers extends AbstractUnivers {

    private final String name;

    public VirtualUnivers(String name) {
        this.name = name;

    }

    public String getDescription() {
        return this.getName();
    }

    public String getId() {
        return "virtual-" + this.getName();
    }

    public String getName() {
        return this.name;
    }

    public String getTitle() {
        return getName();
    }

    public boolean isEqualTo(Univers univers) {
        return univers.getClass() == this.getClass()
                && univers.getName() == getName();
    }

}
