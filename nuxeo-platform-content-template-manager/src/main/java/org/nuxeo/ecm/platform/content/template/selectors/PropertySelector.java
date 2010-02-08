package org.nuxeo.ecm.platform.content.template.selectors;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.content.template.service.ContentFactoryDescriptor;
import org.nuxeo.ecm.platform.content.template.service.FactoryBindingDescriptor;


public class PropertySelector extends AbstractSelector {
    
    public final static String PROPERTY_PATH = "filter-property";
    public final static String PROPERTY_VALUE = "filter-value";
    
    public String getKeyFor(DocumentModel doc) {
        for (Entry entry:entries.values()) {
            String key = getKeyFor(doc, entry);
            if (entries.containsKey(key)) {
                return key;
            }
        }
        return null;
    }
    
    protected static String formatKey(String type, String path, String value) {
        return String.format("%s-%s-%s", type, path, value);
    }
    
    public String getKeyFor(DocumentModel doc, Entry entry) {
        Map<String, String> options = entry.binding.getOptions();
        String path = options.get(PROPERTY_PATH);
        String value;
        try {
            value = doc.getProperty(path).getValue(String.class);
        } catch (ClientException e) {
            return null;
        }
        if (value == null) {
            return null;
        }
        return formatKey(doc.getType(), path, value);
    }
    
    public String getKeyFor(ContentFactoryDescriptor desc, FactoryBindingDescriptor binding) {
        final Map<String, String> options = binding.getOptions();
        if (!options.containsKey(PROPERTY_PATH) ||
                !options.containsKey(PROPERTY_VALUE)) {
            return null;
        }
        if (binding.isTargetFacet()) {
            log.error(binding.getName() + " : unsupported configuration, property selector do not support facet");
            return null;
        }
        return formatKey(binding.getTargetType(), options.get(PROPERTY_PATH), options.get(PROPERTY_VALUE));
    }

    

}
