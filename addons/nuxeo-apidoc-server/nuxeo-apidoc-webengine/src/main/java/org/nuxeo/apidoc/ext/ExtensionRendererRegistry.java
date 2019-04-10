package org.nuxeo.apidoc.ext;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.ext.schema.SchemaRendererFactory;

public class ExtensionRendererRegistry {

    protected static Map<String, ExtensionRendererFactory> registry = new HashMap<String, ExtensionRendererFactory>();

    static {
        registry.put("org.nuxeo.ecm.core.schema.TypeService--schema",
                new SchemaRendererFactory());
    }

    public static ExtensionRenderer getRenderer(ExtensionInfo ei) {
        ExtensionRendererFactory factory = registry.get(ei.getExtensionPoint());
        if (factory != null) {
            return factory.getRendered(ei);
        }
        return null;
    }
}
