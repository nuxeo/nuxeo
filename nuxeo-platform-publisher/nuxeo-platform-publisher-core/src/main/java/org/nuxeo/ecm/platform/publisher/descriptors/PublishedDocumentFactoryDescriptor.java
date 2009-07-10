package org.nuxeo.ecm.platform.publisher.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.io.Serializable;

/**
 * 
 * Descriptor used to register PublishedDocument factories
 * 
 * @author tiry
 * 
 */
@XObject("publishedDocumentFactory")
public class PublishedDocumentFactoryDescriptor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XNode("@name")
    private String name;

    @XNode("@class")
    private Class<? extends PublishedDocumentFactory> klass;

    @XNode("@validatorsRule")
    private String validatorsRuleName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends PublishedDocumentFactory> getKlass() {
        return klass;
    }

    public void setKlass(Class<? extends PublishedDocumentFactory> klass) {
        this.klass = klass;
    }

    public String getValidatorsRuleName() {
        return validatorsRuleName;
    }
    
}
