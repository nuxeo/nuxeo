/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Thibaud Arguillere <targuillere@nuxeo.com>
 */
package org.nuxeo.ecm.platform.tag.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.tag.TagService;

import java.util.Collections;

/**
 * @since 7.1 Tag a document with one or several 'tags'.
 */
@Operation(id = TagDocument.ID, category = Constants.CAT_SERVICES, label = "Tag Document", description = "Tag document with one or "
        + "several 'tags'.", since = "7.1", addToStudio = true)
public class TagDocument {

    public static final String ID = "Services.TagDocument";

    @Context
    protected TagService tagService;

    @Context
    protected CoreSession session;

    @Param(name = "tags", required = true, description = "Labels or tags " + "separated by comma.")
    protected StringList tags;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel document) {
        if (tags != null) {
            tags.removeAll(Collections.singleton(""));
            for (String tag : tags) {
                tagService.tag(session, document.getId(), tag);
            }
        }
        return document;
    }
}
