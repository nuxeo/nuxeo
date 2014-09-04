/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.ecm.platform.tag.automation;

import java.util.Collections;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.Component;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 *
 *
 * @since 5.9.4-JSF2-SNAPSHOT
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

    @OperationMethod
    public Blob run() throws ClientException {
        JSONArray result = new JSONArray();
        if (tagService != null && tagService.isEnabled()) {
            if (!StringUtils.isEmpty(value)) {
                final NavigationContext navigationContext = (NavigationContext) Component.getInstance("navigationContext");
                DocumentModel currentDocument = navigationContext.getCurrentDocument();
                if (currentDocument == null) {
                    return null;
                } else {
                    String docId = currentDocument.getId();
                    List<Tag> tags = tagService.getDocumentTags(
                            documentManager, docId, null);
                    Collections.sort(tags, Tag.LABEL_COMPARATOR);
                    for (Tag tag : tags) {
                        JSONObject obj = new JSONObject();
                        obj.element(Select2Common.ID, tag.getLabel());
                        obj.element(Select2Common.LABEL, tag.getLabel());
                        result.add(obj);
                    }
                }
            } else {
                if (!StringUtils.isBlank(searchTerm)) {
                    List<Tag> tags = tagService.getSuggestions(documentManager,
                            searchTerm, null);
                    Collections.sort(tags, Tag.LABEL_COMPARATOR);
                    for (int i = 0; i < 10 && i < tags.size(); i++) {
                        JSONObject obj = new JSONObject();
                        Tag tag = tags.get(i);
                        obj.element(Select2Common.ID, tag.getLabel());
                        obj.element(Select2Common.LABEL, tag.getLabel());
                        result.add(obj);
                    }
                }
            }
        }
        return new StringBlob(result.toString(), "application/json");
    }

}