package org.nuxeo.template.processors;

import java.util.List;
import java.util.Map;

import org.nuxeo.template.TemplateInput;
import org.nuxeo.template.adapters.doc.TemplateBasedDocument;

public interface InputBindingResolver {

    public abstract void resolve(List<TemplateInput> inputParams,
            Map<String, Object> context,
            TemplateBasedDocument templateBasedDocument);

}