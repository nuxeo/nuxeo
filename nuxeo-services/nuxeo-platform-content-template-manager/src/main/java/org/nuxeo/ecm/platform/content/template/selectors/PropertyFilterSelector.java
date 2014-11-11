package org.nuxeo.ecm.platform.content.template.selectors;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.content.template.service.ContentFactoryDescriptor;
import org.nuxeo.ecm.platform.content.template.service.FactoryBindingDescriptor;

public class PropertyFilterSelector extends AbstractSelector {

    public final static String PROPERTY_PATH = "filter-property";

    public final static String PROPERTY_VALUE = "filter-value";

    protected String getKeyFor(DocumentModel doc, Entry entry) {
        Map<String, String> options = entry.binding.getOptions();
        String path = options.get(PROPERTY_PATH);
        final Property property;
        final String value;
        try {
          property = doc.getProperty(path);
          value = property.getValue(String.class);
        } catch (ClientException e) {
          return null;
        }
        if (value == null) {
          return null;
        }
        String key = formatKey(doc.getType(), path, value);
        if (entries.containsKey(key)) {
          try {
            doc.getProperty(path).setValue(null);
          } catch (ClientException e) {
            log.error("Cannot reset template source " + path + " on " + doc.getPathAsString(), e);
            return null;
          }
          return key;
        }
        return null;
    }

    protected static String formatKey(String type, String path, String value) {
        return String.format("%s-%s-%s", type, path, value);
    }

    public String getKeyFor(ContentFactoryDescriptor desc, FactoryBindingDescriptor binding) {
        final Map<String, String> options = binding.getOptions();
        if (!options.containsKey(PROPERTY_PATH) || !options.containsKey(PROPERTY_VALUE)) {
            return null;
        }
        if (binding.isTargetFacet()) {
            log.error(binding.getName() + " : unsupported configuration, property selector do not support facet");
            return null;
        }
        return formatKey(binding.getTargetType(), options.get(PROPERTY_PATH), options.get(PROPERTY_VALUE));
    }

}
