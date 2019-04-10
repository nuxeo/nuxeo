package org.nuxeo.template.processors.jxls;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.template.processors.AbstractBindingResolver;

public class JXLSBindingResolver extends AbstractBindingResolver {

    @Override
    protected Object handleLoop(String paramName, Object value) {
        return value;
    }

    @Override
    protected Object handlePictureField(String paramName, Blob blobValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void handleBlobField(String paramName, Blob blobValue) {
        // TODO Auto-generated method stub

    }

}
