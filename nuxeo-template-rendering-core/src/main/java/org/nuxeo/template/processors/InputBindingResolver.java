package org.nuxeo.template.processors;

import java.util.List;
import java.util.Map;

import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

public interface InputBindingResolver {

    public abstract void resolve(List<TemplateInput> inputParams, Map<String, Object> context,
            TemplateBasedDocument templateBasedDocument);

}
