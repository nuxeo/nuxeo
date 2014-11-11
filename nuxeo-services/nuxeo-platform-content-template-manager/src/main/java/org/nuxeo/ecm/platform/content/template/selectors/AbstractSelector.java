package org.nuxeo.ecm.platform.content.template.selectors;


import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.content.template.service.ContentFactory;
import org.nuxeo.ecm.platform.content.template.service.ContentFactoryDescriptor;
import org.nuxeo.ecm.platform.content.template.service.FactoryBindingDescriptor;
import org.nuxeo.ecm.platform.content.template.service.FactorySelector;

public abstract class AbstractSelector implements FactorySelector {

    public static final Log log = LogFactory.getLog(TypeSelector.class);

    protected static class Entry {
        public final ContentFactoryDescriptor desc;

        public final FactoryBindingDescriptor binding;

        public final ContentFactory factory;

        Entry(ContentFactoryDescriptor desc, FactoryBindingDescriptor binding, ContentFactory factory) {
            this.desc = desc;
            this.binding = binding;
            this.factory = factory;
        }
    }
    
    protected Map<String, Entry> entries = new HashMap<String, Entry>();
    
    public String getKeyFor(DocumentModel doc) {
        for (Entry entry:entries.values()) {
            String key = getKeyFor(doc, entry);
            if (entries.containsKey(key)) {
                return key;
            }
        }
        return null;
    } 
    
    protected abstract String getKeyFor(DocumentModel doc, Entry entry);
    
    public ContentFactory getFactoryFor(DocumentModel doc) {
        String key = getKeyFor(doc);
        if (key == null) {
            return null;
        }
        return entries.get(key).factory;
    }

    public String register(ContentFactoryDescriptor desc, FactoryBindingDescriptor binding, ContentFactory factory) {
        String key = getKeyFor(desc, binding);
        if (key == null) {
            return null;
        }
        if (entries.containsKey(key)) {
            log.warn(key + " already registered");
            return null;
        }
        Entry entry = new Entry(desc, binding, factory);
        entries.put(key, entry);
        return key;
    }
}
