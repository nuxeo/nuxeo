/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.core.event.async;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for the contribution of an EventTransformer.
 *
 * @since XXX
 */
@XObject("eventTransformer")
public class EventTransformerDescriptor implements Descriptor {

    @XNode("@id")
    protected String id;

    @XNode("@class")
    protected Class<? extends EventTransformer> eventTransformerClass;

    @Override
    public String getId() {
        return id;
    }

    public EventTransformer newInstance() {
        try {
            return eventTransformerClass.getDeclaredConstructor().newInstance().withId(id);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }
}
