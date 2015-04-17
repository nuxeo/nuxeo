package org.nuxeo.ecm.webengine.gwt;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("bundle")
public class GwtWarBundle extends GwtWarLocation {

    @XNode
    public String pathname = "gwt-war";

    @Override
    public String toString() {
        return "GWT War Bundle Location [" + name + "," + pathname + "]";
    }
}
