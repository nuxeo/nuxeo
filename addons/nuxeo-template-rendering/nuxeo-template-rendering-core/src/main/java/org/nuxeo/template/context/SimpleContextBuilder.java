package org.nuxeo.template.context;

import org.nuxeo.template.api.context.DocumentWrapper;

public class SimpleContextBuilder extends AbstractContextBuilder {

    DocumentWrapper wrapper = new SimpleBeanWrapper();

    @Override
    protected DocumentWrapper getWrapper() {
        return wrapper;
    }

}
