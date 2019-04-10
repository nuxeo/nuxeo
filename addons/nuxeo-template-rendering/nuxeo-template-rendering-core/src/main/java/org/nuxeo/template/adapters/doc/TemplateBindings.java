package org.nuxeo.template.adapters.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public class TemplateBindings extends ArrayList<TemplateBinding> {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_BINDING = "default";

    public static final String BINDING_PROP_NAME = "nxts:bindings";

    public TemplateBindings(DocumentModel doc) throws ClientException {
        Serializable value = doc.getPropertyValue(BINDING_PROP_NAME);
        if (value != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Serializable>> bindings = (List<Map<String, Serializable>>) value;
            for (Map<String, Serializable> binding : bindings) {
                add(new TemplateBinding(binding));
            }
        }
    }

    public String useMainContentAsTemplate() {
        for (TemplateBinding tb : this) {
            if (tb.isUseMainContentAsTemplate()) {
                return tb.getName();
            }
        }
        return null;
    }

    public TemplateBinding get() {
        return get(DEFAULT_BINDING);
    }

    public void removeByName(String templateName) {
        Iterator<TemplateBinding> it = this.iterator();
        while (it.hasNext()) {
            TemplateBinding binding = it.next();
            if (binding.getName().equals(templateName)) {
                it.remove();
                return;
            }
        }
    }

    public boolean containsTemplateName(String templateName) {
        for (TemplateBinding tb : this) {
            if (templateName.equals(tb.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean containsTemplateId(String templateId) {
        for (TemplateBinding tb : this) {
            if (templateId.equals(tb.getTemplateId())) {
                return true;
            }
        }
        return false;
    }

    public TemplateBinding get(String name) {
        for (TemplateBinding tb : this) {
            if (name.equals(tb.getName())) {
                return tb;
            }
        }
        return null;
    }

    public void addOrUpdate(TemplateBinding tb) {
        TemplateBinding existing = get(tb.getName());
        if (existing == null) {
            add(tb);
        } else {
            existing.update(tb);
        }
    }

    public List<String> getNames() {

        List<String> names = new ArrayList<String>();
        for (TemplateBinding tb : this) {
            names.add(tb.getName());
        }
        return names;
    }

    public void save(DocumentModel doc) throws ClientException {
        List<Map<String, Serializable>> bindings = new ArrayList<Map<String, Serializable>>();
        for (TemplateBinding tb : this) {
            bindings.add(tb.getAsMap());
        }
        doc.setPropertyValue(BINDING_PROP_NAME, (Serializable) bindings);
    }
}
