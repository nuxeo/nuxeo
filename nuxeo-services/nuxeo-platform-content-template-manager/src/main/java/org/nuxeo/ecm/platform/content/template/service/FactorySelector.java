package org.nuxeo.ecm.platform.content.template.service;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface FactorySelector {
    
    String getKeyFor(DocumentModel doc);
    
    String getKeyFor(ContentFactoryDescriptor desc, FactoryBindingDescriptor binding);
    
    ContentFactory getFactoryFor(DocumentModel doc);
   
    String register(ContentFactoryDescriptor desc, FactoryBindingDescriptor binding, ContentFactory factory);

}
