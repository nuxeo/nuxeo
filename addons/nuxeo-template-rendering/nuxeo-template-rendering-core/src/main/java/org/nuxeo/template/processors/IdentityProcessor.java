package org.nuxeo.template.processors;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

public class IdentityProcessor extends AbstractTemplateProcessor {

    public static final String NAME = "Identity";

    @Override
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument,
            String templateName) throws Exception {
        return getSourceTemplateBlob(templateBasedDocument, templateName);
    }

    @Override
    public List<TemplateInput> getInitialParametersDefinition(Blob blob)
            throws Exception {
        return new ArrayList<TemplateInput>();
    }

}
