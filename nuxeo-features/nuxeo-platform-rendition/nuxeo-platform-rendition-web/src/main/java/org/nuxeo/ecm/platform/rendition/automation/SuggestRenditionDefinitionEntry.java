/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.rendition.automation;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;
import org.nuxeo.runtime.api.Framework;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @since 8.3
 */
@Operation(id = SuggestRenditionDefinitionEntry.ID, category = Constants.CAT_SERVICES, label = "Get rendition definition suggestion", description = "Get rendition definition suggestion")
public class SuggestRenditionDefinitionEntry {

    public static final String ID = "RenditionDefinition.Suggestion";

    public static final int MAX_SUGGESTIONS = 10;

    public static final Comparator<RenditionDefinition> LABEL_COMPARATOR = new RenditionDefinitionComparator();

    @Param(name = "searchTerm", required = false)
    protected String searchTerm;

    @OperationMethod
    public Blob run() {
        JSONArray result = new JSONArray();
        if (!StringUtils.isBlank(searchTerm)) {
            for (RenditionDefinition def : getSuggestions(searchTerm)) {
                JSONObject obj = new JSONObject();
                String name = def.getName();
                obj.element(Select2Common.ID, name);
                obj.element(Select2Common.LABEL, name);
                result.add(obj);
            }
        }
        return Blobs.createJSONBlob(result.toString());
    }

    protected List<RenditionDefinition> getSuggestions(String searchTerm) {
        RenditionService renditionService = Framework.getService(RenditionService.class);
        List<RenditionDefinition> defs = renditionService.getDeclaredRenditionDefinitions();
        return defs.stream().filter(def -> def.getName().startsWith(searchTerm)).sorted(LABEL_COMPARATOR).limit(
                MAX_SUGGESTIONS).collect(Collectors.toList());
    }

    protected static class RenditionDefinitionComparator implements Comparator<RenditionDefinition> {
        @Override
        public int compare(RenditionDefinition o1, RenditionDefinition o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }

}
