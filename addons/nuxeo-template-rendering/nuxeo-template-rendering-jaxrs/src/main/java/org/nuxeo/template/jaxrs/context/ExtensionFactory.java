package org.nuxeo.template.jaxrs.context;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.context.ContextExtensionFactory;
import org.nuxeo.template.api.context.DocumentWrapper;

public class ExtensionFactory implements ContextExtensionFactory {

    @Override
    public Object getExtension(DocumentModel doc, DocumentWrapper wrapper,
            Map<String, Object> ctx) {
        return new JAXRSExtensions(doc, wrapper,
                (String) ctx.get("templateName"));
    }

}
