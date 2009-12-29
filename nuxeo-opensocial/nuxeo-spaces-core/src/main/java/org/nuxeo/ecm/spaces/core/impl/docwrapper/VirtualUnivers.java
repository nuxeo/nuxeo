package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.spaces.api.Univers;

public class VirtualUnivers implements Univers {

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
        // TODO Auto-generated method stub
        return getName();
    }

    public boolean isEqualTo(Univers univers) {
        return univers.getClass() == this.getClass() && univers.getName() == getName();
    }

}
