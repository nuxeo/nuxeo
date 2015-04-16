package org.nuxeo.ecm.webengine.gwt;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("bundleLocation")
public class GwtAppBundleLocation {

    @XNode("@application")
    public String name = "unknown";

    @XNode("pathname")
    public String pathname = "gwt-war";

    @Override
    public String toString() {
        return "GWT Bundle location [" + name + "," + pathname + "]";
    }
}
