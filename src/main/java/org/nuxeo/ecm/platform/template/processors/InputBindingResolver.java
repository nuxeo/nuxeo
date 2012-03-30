package org.nuxeo.ecm.platform.template.processors;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;

public interface InputBindingResolver {

    public abstract void resolve(List<TemplateInput> inputParams,
            Map<String, Object> context,
            TemplateBasedDocument templateBasedDocument);

}