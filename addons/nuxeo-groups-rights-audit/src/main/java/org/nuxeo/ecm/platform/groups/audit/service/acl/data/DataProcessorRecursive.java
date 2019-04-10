/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;

/**
 * Gather various data and statistics about a document tree
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class DataProcessorRecursive extends DataProcessor implements IDataProcessor {
    public DataProcessorRecursive(IContentFilter filter) {
        super(filter);
    }

    /** {@inheritDoc}. timeout ignored */
    @Override
    public void analyze(CoreSession session, DocumentModel doc, int timeout) throws ClientException {
        init();
        doAnalyze(session, doc, 0);
        log();
    }

    /**
     * Analyze recursively the document tree. After calling this method, on can retrieve:
     * <ul>
     * <li>the tree depth
     * <li>all user and groups mentioned in the documents' ACLs
     * <li>all permission names mentioned in the documents' ACLs
     * </ul>
     * Note that root is considered as a document, so a repository made of: <code>
     * <pre>
     *  /
     *  |-folder1
     *  |-folder2
     * </pre>
     * </code> has a depth of 2. Once called, the method erase previous results.
     *
     * @param session
     * @param doc
     * @throws ClientException
     */
    protected void doAnalyze(CoreSession session, DocumentModel doc, int depth) throws ClientException {
        initSummarySet();
        processDocument(doc);

        // continue working recursively
        DocumentModelList list = session.getChildren(doc.getRef());
        for (DocumentModel child : list) {
            doAnalyze(session, child, depth + 1);
        }
    }
}
