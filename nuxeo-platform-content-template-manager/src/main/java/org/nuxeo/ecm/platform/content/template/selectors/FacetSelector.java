package org.nuxeo.ecm.platform.content.template.selectors;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.content.template.service.ContentFactoryDescriptor;
import org.nuxeo.ecm.platform.content.template.service.FactoryBindingDescriptor;


public class FacetSelector extends AbstractSelector {

    public String getKeyFor(DocumentModel doc, Entry entry) {
        for (String facet:doc.getDeclaredFacets()) {
            if (facet.equals(getKeyFor(entry.desc, entry.binding))) {
                return facet;
            }
        }
        return null;
    }

    public String getKeyFor(ContentFactoryDescriptor desc, FactoryBindingDescriptor binding) {
        return binding.getTargetFacet();
    }

}
