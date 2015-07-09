/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.admin.rights;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * @since 7.4
 */
public class RightsPurgeWork extends AbstractWork {

    public static final int DEFAULT_BATCH_SIZE = 20;

    public static final String CATEGORY = "rightsPurge";

    protected DocumentModel searchDocument;

    protected int batchSize = DEFAULT_BATCH_SIZE;

    public RightsPurgeWork(DocumentModel searchDocument) {
        this.searchDocument = searchDocument;
    }

    @Override
    public String getTitle() {
        return String.format("Rights purge for: %s, %s", searchDocument.getPropertyValue("rs:ace_username"),
                searchDocument.getPropertyValue("rs:ecm_path"));
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public void work() {
        setStatus("Purging");
        initSession();

        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        PageProviderDefinition def = pageProviderService.getPageProviderDefinition("rights_purge");
        String query = NXQLQueryBuilder.getQuery(searchDocument, def.getWhereClause(), null);

        IterableQueryResult result = session.queryAndFetch(query, NXQL.NXQL);
        List<String> docIds = new ArrayList<>();
        try {
            for (Map<String, Serializable> map : result) {
                docIds.add((String) map.get("ecm:uuid"));
            }
        } finally {
            result.close();
        }

        List<String> usernames = (List<String>) searchDocument.getPropertyValue("rs:ace_username");
        int acpUpdatedCount = 0;
        for (String docId : docIds) {
            DocumentRef ref = new IdRef(docId);
            ACP acp = session.getACP(ref);
            // cleanup acp for all principals
            boolean changed = false;
            for (String username : usernames) {
                if (acp.removeACEsByUsername(username)) {
                    changed = true;
                }
            }

            try {
                if (changed) {
                    session.setACP(ref, acp, true);
                    acpUpdatedCount++;
                    if (acpUpdatedCount % batchSize == 0) {
                        commitOrRollbackTransaction();
                        startTransaction();
                    }
                }
            } catch (TransactionRuntimeException e) {
                if (e.getMessage().contains("Transaction timeout")) {
                    batchSize = 1;
                }
                throw e;
            }

        }
        setStatus(null);
    }

    @Override
    public int getRetryCount() {
        return 10;
    }
}
