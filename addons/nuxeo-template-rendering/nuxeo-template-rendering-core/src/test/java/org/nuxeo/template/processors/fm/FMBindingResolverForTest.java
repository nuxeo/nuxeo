package org.nuxeo.template.processors.fm;

import org.nuxeo.ecm.core.api.Blob;

public class FMBindingResolverForTest extends FMBindingResolver {

    @Override
    protected Object handleLoop(String paramName, Object value) {
        return super.handleLoop(paramName, value);
    }

    @Override
    protected Object handlePictureField(String paramName, Blob blobValue) {
        return super.handlePictureField(paramName, blobValue);
    }

    @Override
    protected void handleBlobField(String paramName, Blob blobValue) {
        super.handleBlobField(paramName, blobValue);
    }

    @Override
    protected String handleHtmlField(String paramName, String htmlValue) {
        return super.handleHtmlField(paramName, htmlValue);
    }
}
