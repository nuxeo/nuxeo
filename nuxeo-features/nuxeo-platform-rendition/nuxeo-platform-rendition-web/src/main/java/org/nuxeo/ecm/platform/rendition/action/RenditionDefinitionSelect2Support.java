/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.rendition.action;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.jboss.seam.ScopeType.EVENT;

/**
 * Helper component for rendition name widget relying on select2.
 *
 * @since 8.3
 */
@Name("renditionDefinitionSelect2Support")
@Scope(EVENT)
public class RenditionDefinitionSelect2Support {

    protected String label;

    public String resolveRenditionDefinitions(final List<String> list) {
        return Select2Common.resolveDefaultEntries(list);
    }

    public String resolveRenditionDefinitions(final String[] array) {
        if (array == null || array.length == 0) {
            return Select2Common.resolveDefaultEntries(null);
        }
        return Select2Common.resolveDefaultEntries(Arrays.asList(array));
    }

    protected void reset() {
        label = null;
    }

    public String encodeParameters(final Map<String, Serializable> widgetProperties) {
        return encodeCommonParameters(widgetProperties).toString();
    }

    protected JSONObject encodeCommonParameters(final Map<String, Serializable> widgetProperties) {
        return encodeCommonParameters(widgetProperties, null);
    }

    protected JSONObject encodeCommonParameters(final Map<String, Serializable> widgetProperties,
            final Map<String, String> additionalParameters) {
        JSONObject obj = new JSONObject();
        obj.put("multiple", "true");
        obj.put(Select2Common.MIN_CHARS, "1");
        obj.put(Select2Common.READ_ONLY_PARAM, "false");
        obj.put(Select2Common.OPERATION_ID, "RenditionDefinition.Suggestion");
        obj.put(Select2Common.WIDTH, "300px");
        obj.put(Select2Common.SELECTION_FORMATTER, "formatSelectedRenditionDefinitions");
        obj.put(Select2Common.SUGGESTION_FORMATTER, "formatSuggestedRenditionDefinitions");
        JSONArray tokenSeparator = new JSONArray();
        tokenSeparator.add(",");
        tokenSeparator.add(" ");
        obj.put("tokenSeparators", tokenSeparator);
        if (additionalParameters != null) {
            for (Entry<String, String> entry : additionalParameters.entrySet()) {
                obj.put(entry.getKey(), entry.getValue().toString());
            }
        }
        for (Entry<String, Serializable> entry : widgetProperties.entrySet()) {
            obj.put(entry.getKey(), entry.getValue().toString());
        }
        return obj;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}