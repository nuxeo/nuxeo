package org.nuxeo.ecm.platform.content.template.selectors;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.content.template.service.ContentFactoryDescriptor;
import org.nuxeo.ecm.platform.content.template.service.FactoryBindingDescriptor;


public class TypeSelector extends AbstractSelector{

    public String getKeyFor(DocumentModel doc, Entry entry) {
        return doc.getType();
    }

    public String getKeyFor(ContentFactoryDescriptor desc, FactoryBindingDescriptor binding) {
        return binding.getTargetType();   
    }
 
}
