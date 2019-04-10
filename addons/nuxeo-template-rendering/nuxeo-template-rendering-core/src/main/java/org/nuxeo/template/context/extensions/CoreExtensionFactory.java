package org.nuxeo.template.context.extensions;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.context.ContextExtensionFactory;
import org.nuxeo.template.api.context.DocumentWrapper;

public class CoreExtensionFactory implements ContextExtensionFactory {

    @Override
    public Object getExtension(DocumentModel currentDocument,
            DocumentWrapper wrapper, Map<String, Object> ctx) {
        return new CoreExtensions(currentDocument, wrapper);
    }

}
