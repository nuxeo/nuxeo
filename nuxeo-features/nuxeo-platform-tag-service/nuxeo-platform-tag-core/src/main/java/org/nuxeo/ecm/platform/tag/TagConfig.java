package org.nuxeo.ecm.platform.tag;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "config")
public class TagConfig  {
    
    @XNode(value = "queryProxy")
    protected boolean queryProxy = true;
    
    public boolean isQueryingForProxy() {
        return queryProxy;
    }

}
