/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.seam;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.webapp.bulkedit.BulkEditActions;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.2
 */
@Name("damBulkEditActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class DamBulkEditActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient CoreSession documentManager;

    @Observer(BulkEditActions.SELECTION_EDITED)
    @SuppressWarnings("unchecked")
    public void addTagsOnSelection(List<DocumentModel> selectedDocuments, DocumentModel bulkEditDoc)
            throws ClientException {
        List<String> tags = (List<String>) bulkEditDoc.getContextData(ScopeType.REQUEST, "dam_bulk_edit_tags");
        if (tags != null && !tags.isEmpty()) {
            TagService tagService = Framework.getLocalService(TagService.class);
            String username = documentManager.getPrincipal().getName();
            for (DocumentModel doc : selectedDocuments) {
                for (String tag : tags) {
                    tagService.tag(documentManager, doc.getId(), tag, username);
                }
            }
        }
    }

}
