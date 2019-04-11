/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

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
    public void analyze(CoreSession session, DocumentModel doc, int timeout) {
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
     */
    @Override
    protected void doAnalyze(CoreSession session, DocumentModel doc, int depth) {
        initSummarySet();
        processDocument(doc);

        // continue working recursively
        DocumentModelList list = session.getChildren(doc.getRef());
        for (DocumentModel child : list) {
            doAnalyze(session, child, depth + 1);
        }
    }
}
