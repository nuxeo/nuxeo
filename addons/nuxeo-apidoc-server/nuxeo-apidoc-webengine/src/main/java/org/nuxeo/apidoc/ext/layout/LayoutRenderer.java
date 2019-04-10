package org.nuxeo.apidoc.ext.layout;

import java.io.IOException;

import org.dom4j.Document;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.ext.AbstractExtensionRenderer;
import org.nuxeo.apidoc.ext.ExtensionRenderer;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.ecm.core.api.Blob;

public class LayoutRenderer extends AbstractExtensionRenderer implements
        ExtensionRenderer {

    public LayoutRenderer(ExtensionInfo ei) {
        super(ei);
    }

    @Override
    public String getViewName() {
        return "view_schema";
    }

    public Object getRenderObject(Document xmlContrib) {
        SchemaRenderObject result = new SchemaRenderObject();

        result.src = xmlContrib.getRootElement().attribute("src").getValue();
        if (xmlContrib.getRootElement().attribute("prefix") != null) {
            result.prefix = xmlContrib.getRootElement().attribute("prefix").getValue();
        }
        result.name = xmlContrib.getRootElement().attribute("name").getValue();

        BundleInfo bi = ei.getComponent().getBundle();
        if (bi instanceof BundleInfoImpl) {
            try {
                Blob xsdBlob = ((BundleInfoImpl) bi).getResource(result.src);
                String xsd = xsdBlob.getString();
                result.json = XSD2JSON.asJSON(result.name, result.prefix, xsd);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return result;
    }

}
