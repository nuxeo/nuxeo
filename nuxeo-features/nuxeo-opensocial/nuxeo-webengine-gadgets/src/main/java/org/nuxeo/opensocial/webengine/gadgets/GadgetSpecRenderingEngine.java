package org.nuxeo.opensocial.webengine.gadgets;

import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;

import freemarker.cache.StringTemplateLoader;

public class GadgetSpecRenderingEngine extends FreemarkerEngine {

    public GadgetSpecRenderingEngine(StringTemplateLoader specLoader) {
        super();
        cfg.setTemplateLoader(specLoader);
    }

}
