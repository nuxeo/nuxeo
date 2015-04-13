package org.nuxeo.ecm.webengine.gwt;

import java.io.File;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("location")
public class GwtAppLocation {

    @XNode("@application")
    public String name = "unknown";

    @XNode("dir")
    public File dir = new File("/dev/null");

    @Override
    public String toString() {
        return "GWT location [" + name + "," + dir + "]";
    }
}
