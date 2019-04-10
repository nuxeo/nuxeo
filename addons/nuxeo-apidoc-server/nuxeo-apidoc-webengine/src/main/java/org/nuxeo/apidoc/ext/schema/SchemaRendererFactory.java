package org.nuxeo.apidoc.ext.schema;

import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.ext.ExtensionRenderer;
import org.nuxeo.apidoc.ext.ExtensionRendererFactory;

public class SchemaRendererFactory implements ExtensionRendererFactory {

    @Override
    public ExtensionRenderer getRendered(ExtensionInfo ei) {
        return new SchemaRenderer(ei);
    }

}
