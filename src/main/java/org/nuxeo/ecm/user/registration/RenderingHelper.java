package org.nuxeo.ecm.user.registration;

import java.io.File;
import java.net.URL;

import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;

public class RenderingHelper {

    protected static RenderingEngine engine;

    protected class CLResourceLocator implements ResourceLocator {
        public File getResourceFile(String key) {
            return null;
        }
        public URL getResourceURL(String key) {
            return this.getClass().getClassLoader().getResource(key);
        }
    }

    public RenderingEngine getRenderingEngine() {
        if (engine==null) {
            engine = new FreemarkerEngine();
            engine.setResourceLocator(new CLResourceLocator());
        }
        return engine;
    }

}
