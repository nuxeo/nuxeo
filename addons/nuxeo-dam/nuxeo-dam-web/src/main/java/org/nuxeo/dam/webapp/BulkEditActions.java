/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.dam.webapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.dam.webapp.chainselect.ChainSelectCleaner;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

import static org.jboss.seam.ScopeType.CONVERSATION;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("bulkEditActions")
@Scope(CONVERSATION)
public class BulkEditActions extends
        org.nuxeo.ecm.webapp.bulkedit.BulkEditActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    @Factory(value = "bulkEditDocumentModel", scope = ScopeType.EVENT)
    public DocumentModel getBulkEditDocumentModel() {
        if (fictiveDocumentModel == null) {
            // add all available schemas to the DocumentModel
            SchemaManager schemaManager = null;
            try {
                schemaManager = Framework.getService(SchemaManager.class);
            } catch (Exception e) {

            }
            List<String> schemas = new ArrayList<String>();
            if (schemaManager != null) {
                for (Schema schema : schemaManager.getSchemas()) {
                    schemas.add(schema.getName());
                }
            }
            fictiveDocumentModel = FictiveDocumentModel .createFictiveDocumentModelWith(schemas);
        }
        return fictiveDocumentModel;
    }

    @Override
    public void bulkEditSelectionNoRedirect() throws ClientException {
        super.bulkEditSelectionNoRedirect();
        cleanupChainSelects();
    }

    @Override
    public void cancel() {
        super.cancel();
        cleanupChainSelects();
    }

    protected void cleanupChainSelects() {
        ChainSelectCleaner.cleanup(ChainSelectCleaner.BULK_EDIT_COVERAGE_CHAIN_SELECT_ID);
        ChainSelectCleaner.cleanup(ChainSelectCleaner.BULK_EDIT_SUBJECTS_CHAIN_SELECT_ID);
    }

}
