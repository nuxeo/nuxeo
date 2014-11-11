package org.nuxeo.ecm.platform.publisher.descriptors;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinderFactory;

/**
 * Descriptor used to register {@link RootSectionFinder} factories.
 * 
 * @author tiry
 */
@XObject("rootSectionFinderFactory")
public class RootSectionFinderFactoryDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@class")
    private Class<? extends RootSectionFinderFactory> factory;

    public Class<? extends RootSectionFinderFactory> getFactory() {
        return factory;
    }
}
