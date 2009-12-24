package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.spaces.api.Univers;

public class VirtualUnivers implements Univers {

    /**
     *
     */
    private static final String id = "4231218851217426210";
    private final String name;

    public VirtualUnivers(String name) {
        this.name = name;

    }

    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getId() {
        return id;
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
