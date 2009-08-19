package org.nuxeo.ecm.platform.publisher.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;

import java.io.Serializable;

/**
 * 
 * Descriptor of a PublicationTree
 * 
 * @author tiry
 * 
 */
@XObject("publicationTree")
public class PublicationTreeDescriptor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XNode("@name")
    private String name;

    @XNode("@factory")
    private String factory;

    @XNode("@class")
    private Class<? extends PublicationTree> klass;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public Class<? extends PublicationTree> getKlass() {
        return klass;
    }

    public void setKlass(Class<? extends PublicationTree> klass) {
        this.klass = klass;
    }

}
