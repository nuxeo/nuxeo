package org.nuxeo.ecm.platform.wi.backend;

import org.apache.commons.lang.StringUtils;


/**
 * @author Organization: Gagnavarslan ehf
 */
public class SimpleRootBackend extends SimpleBackend {

    public SimpleRootBackend() {
        super("", "", "");
        this.rootPath = "/default-domain";
    }

    @Override
    public boolean isRoot() {
        return true;
    }


}
