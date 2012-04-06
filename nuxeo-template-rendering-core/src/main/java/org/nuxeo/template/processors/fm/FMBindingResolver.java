package org.nuxeo.template.processors.fm;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.template.processors.AbstractBindingResolver;

import freemarker.template.TemplateModelException;

public class FMBindingResolver extends AbstractBindingResolver {

    @Override
    protected Object handleLoop(String paramName, Object value) {
        try {
            return getWrapper().wrap(value);
        } catch (TemplateModelException e) {
            return null;
        }
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

    @Override
    protected void handleHtmlField(String paramName, String htmlValue) {
        // TODO Auto-generated method stub

    }

}
