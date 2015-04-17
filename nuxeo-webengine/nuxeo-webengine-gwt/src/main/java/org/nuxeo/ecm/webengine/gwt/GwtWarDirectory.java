package org.nuxeo.ecm.webengine.gwt;

import java.io.File;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("directory")
public class GwtWarDirectory extends GwtWarLocation {

    @XNode
    public File dir = new File("/dev/null");

    @Override
    public String toString() {
        return "GWT War Directory [" + name + "," + dir + "]";
    }
}
