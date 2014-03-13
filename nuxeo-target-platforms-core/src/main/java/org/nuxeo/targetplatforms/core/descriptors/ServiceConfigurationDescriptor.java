/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Service configuration descriptor
 *
 * @since 2.18
 */
@XObject("configuration")
public class ServiceConfigurationDescriptor {

    @XNode("defaultTargetPlatform")
    String defaultTargetPlatform;

    public String getDefaultTargetPlatform() {
        return defaultTargetPlatform;
    }

}
