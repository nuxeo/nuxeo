/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.ecm.platform.tag.automation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.features.SuggestConstants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.tag.TagService;

/**
 * @since 6.0
 */
@Operation(id = SuggestTagEntry.ID, category = Constants.CAT_SERVICES, label = "Get tag suggestion", description = "Get tag suggestion")
public class SuggestTagEntry {

    public static final String ID = "Tag.Suggestion";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession documentManager;

    @Context
    protected TagService tagService;

    @Param(name = "searchTerm", required = false)
    protected String searchTerm;

    @Param(name = "value", required = false)
    protected String value;

    @Param(name = "xpath", required = false)
    protected String xpath;

    @Param(name = "document", required = false)
    protected DocumentModel doc;

    @OperationMethod
    public Blob run() throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        if (tagService != null && tagService.isEnabled()) {
            if (!StringUtils.isEmpty(value)) {
                if (doc == null) {
                    return null;
                } else {
                    String docId = doc.getId();
                    List<String> tags = new ArrayList<>(tagService.getTags(documentManager, docId));
                    Collections.sort(tags);
                    for (String tag : tags) {
                        Map<String, String> obj = new LinkedHashMap<>();
                        obj.put(SuggestConstants.ID, tag);
                        obj.put(SuggestConstants.LABEL, tag);
                        result.add(obj);
                    }
                }
            } else {
                if (!StringUtils.isBlank(searchTerm)) {
                    List<String> tags = new ArrayList<>(tagService.getSuggestions(documentManager, searchTerm));
                    Collections.sort(tags);
                    for (int i = 0; i < 10 && i < tags.size(); i++) {
                        Map<String, String> obj = new LinkedHashMap<>();
                        String tag = tags.get(i);
                        obj.put(SuggestConstants.ID, tag);
                        obj.put(SuggestConstants.LABEL, tag);
                        result.add(obj);
                    }
                }
            }
        }
        return Blobs.createJSONBlobFromValue(result);
    }

}
