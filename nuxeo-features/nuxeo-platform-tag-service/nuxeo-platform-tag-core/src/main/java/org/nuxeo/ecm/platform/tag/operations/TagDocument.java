/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.tag.TagService;

/**
 * @since 7.1
 * Tag a document with one or several 'tags'.
 */
@Operation(id = TagDocument.ID, category = Constants.CAT_SERVICES,
        label = "Tag Document", description = "Tag document with one or " +
        "several 'tags'.",
        since = "7.1", addToStudio = true)
public class TagDocument {

    public static final String ID = "Services.TagDocument";

    @Context
    protected TagService tagService;

    @Context
    protected CoreSession session;

    @Param(name = "tags", required = true, description = "Labels or tags " +
            "separated by comma.")
    protected StringList tags;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel document) throws ClientException {
        if (tags != null) {
            for (String tag : tags) {
                tagService.tag(session, document.getId(), tag,
                        session.getPrincipal().getName());
            }
        }
        return document;
    }
}
