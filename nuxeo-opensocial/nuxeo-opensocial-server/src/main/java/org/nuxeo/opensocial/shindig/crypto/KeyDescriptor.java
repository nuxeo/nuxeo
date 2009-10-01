package org.nuxeo.opensocial.shindig.crypto;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("key")
public class KeyDescriptor {
    @XNode("@container")
    private String container;

    @XNode("value")
    private String key;

    public String getContainer() {
        return container;
    }

    public String getKey() {
        return key;
    }

}
