package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

@XObject("factory")
public class SpaceFactoryDescriptor {

    @XNode("@type")
    private String type;

    @XNode("@class")
    private Class<? extends DocumentAdapterFactory> klass;

    public String getType() {
        return type;
    }

    public Class<? extends DocumentAdapterFactory> getKlass() {
        return klass;
    }

}
