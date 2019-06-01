/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.template.adapters.doc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.serializer.service.TemplateSerializerService;

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
        String xml = Framework.getService(TemplateSerializerService.class).serializeXML(params);
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
        Map<String, Serializable> map = new HashMap<>();
        map.put(TEMPLATE_NAME_KEY, name);
        map.put(TEMPLATE_ID_KEY, templateId);
        map.put(TEMPLATE_DATA_KEY, data);
        map.put(TEMPLATE_USE_BLOB_KEY, useMainContentAsTemplate);
        return map;
    }
}
