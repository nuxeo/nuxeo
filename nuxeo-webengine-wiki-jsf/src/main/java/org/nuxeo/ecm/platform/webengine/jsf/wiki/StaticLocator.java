package org.nuxeo.ecm.platform.webengine.jsf.wiki;

import java.io.File;
import java.net.URL;

import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;

public class StaticLocator implements ResourceLocator {

    public URL getResourceURL(String key) {
        return ResourceLocator.class.getClassLoader().getResource(key);
    }

    public File getResourceFile(String key) {
        return null;
    }

}
