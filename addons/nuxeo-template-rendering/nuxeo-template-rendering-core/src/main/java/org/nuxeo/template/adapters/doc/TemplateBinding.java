package org.nuxeo.template.adapters.doc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.template.XMLSerializer;
import org.nuxeo.template.api.TemplateInput;

public class TemplateBinding {

    public static final String TEMPLATE_NAME_KEY = "templateName";

    public static final String TEMPLATE_DATA_KEY = "templateData";

    public static final String TEMPLATE_ID_KEY = "templateId";

    public static final String TEMPLATE_USE_BLOB_KEY = "useMainContentAsTemplate";

    private String name;

    private String templateId;

    private String data;

    private boolean useMainContentAsTemplate;

    public TemplateBinding() {
    }

    public TemplateBinding(Map<String, Serializable> map) {
        name = (String) map.get(TEMPLATE_NAME_KEY);
        templateId = (String) map.get(TEMPLATE_ID_KEY);
        data = (String) map.get(TEMPLATE_DATA_KEY);
        if (map.get(TEMPLATE_USE_BLOB_KEY) != null) {
            useMainContentAsTemplate = (Boolean) map.get(TEMPLATE_USE_BLOB_KEY);
        } else {
            useMainContentAsTemplate = false;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setData(List<TemplateInput> params) {
        String xml = XMLSerializer.serialize(params);
        setData(xml);
    }

    public boolean isUseMainContentAsTemplate() {
        return useMainContentAsTemplate;
    }

    public void setUseMainContentAsTemplate(boolean useMainContentAsTemplate) {
        this.useMainContentAsTemplate = useMainContentAsTemplate;
    }

    public void update(TemplateBinding other) {
        name = other.name;
        templateId = other.templateId;
        data = other.data;
        useMainContentAsTemplate = other.useMainContentAsTemplate;
    }

    public Map<String, Serializable> getAsMap() {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put(TEMPLATE_NAME_KEY, name);
        map.put(TEMPLATE_ID_KEY, templateId);
        map.put(TEMPLATE_DATA_KEY, data);
        map.put(TEMPLATE_USE_BLOB_KEY, useMainContentAsTemplate);
        return map;
    }
}
