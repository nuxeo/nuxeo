/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Thibaud Arguillere <targuillere@nuxeo.com>
 */
package org.nuxeo.ecm.platform.tag.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.tag.TagService;

/**
 * @since 7.1 Remove document tags.
 */
@Operation(id = RemoveDocumentTags.ID, category = Constants.CAT_SERVICES, label = "Tag Document", description = "Remove all document tags.", since = "7.1", addToStudio = true)
public class RemoveDocumentTags {

    public static final String ID = "Services.RemoveDocumentTags";

    @Context
    protected TagService tagService;

    @Context
    protected CoreSession session;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel document) throws ClientException {
        tagService.removeTags(session, document.getId());
        return document;
    }
}
