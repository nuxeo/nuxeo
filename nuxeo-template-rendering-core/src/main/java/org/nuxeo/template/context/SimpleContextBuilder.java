package org.nuxeo.template.context;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

public class SimpleContextBuilder extends AbstractContextBuilder {

    DocumentWrapper wrapper = new SimpleBeanWrapper();

    @Override
    public Map<String, Object> build(DocumentModel doc) throws Exception {
        return build(doc, wrapper);
    }

}
