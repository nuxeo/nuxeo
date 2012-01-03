/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota.count;

import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;
import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY;
import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY;
import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_FACET;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.quota.AbstractQuotaStatsUpdater;

/**
 * {@link org.nuxeo.ecm.quota.QuotaStatsUpdater} counting the non folderish
 * documents.
 * <p>
 * Store the descendant and children count on {@code Folderish} documents.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class DocumentsCountUpdater extends AbstractQuotaStatsUpdater {

    @Override
    protected void processDocumentCreated(CoreSession session, DocumentModel doc)
            throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
    }

    @Override
    protected void processDocumentCopied(CoreSession session, DocumentModel doc)
            throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
    }

    @Override
    protected void processDocumentUpdated(CoreSession session, DocumentModel doc) {
    }

    @Override
    protected void processDocumentMoved(CoreSession session, DocumentModel doc,
            DocumentModel sourceParent) throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        List<DocumentModel> sourceAncestors = getAncestors(session,
                sourceParent);
        sourceAncestors.add(0, sourceParent);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
        updateCountStatistics(session, doc, sourceAncestors, -docCount);
    }

    @Override
    protected void processDocumentAboutToBeRemoved(CoreSession session,
            DocumentModel doc) throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, -docCount);
    }

    protected void updateCountStatistics(CoreSession session,
            DocumentModel doc, List<DocumentModel> ancestors, long count)
            throws ClientException {
        if (ancestors == null || ancestors.isEmpty()) {
            return;
        }

        if (!doc.hasFacet(FOLDERISH)) {
            DocumentModel parent = ancestors.get(0);
            updateParentChildrenCount(session, parent, count);
        }

        for (DocumentModel ancestor : ancestors) {
            if (count != 0) {
                long descendantsCount = 0;
                if (ancestor.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
                    Long c = (Long) ancestor.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY);
                    descendantsCount = c != null ? c : 0;
                }
                descendantsCount += count;

                if (descendantsCount >= 0) {
                    if (!ancestor.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
                        ancestor.addFacet(DOCUMENTS_COUNT_STATISTICS_FACET);
                    }
                    ancestor.setPropertyValue(
                            DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY,
                            descendantsCount);
                } else {
                    if (ancestor.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
                        ancestor.removeFacet(DOCUMENTS_COUNT_STATISTICS_FACET);
                    }
                }
                session.saveDocument(ancestor);
            }
        }
        session.save();
    }

    protected void updateParentChildrenCount(CoreSession session,
            DocumentModel parent, long count) throws ClientException {
        long childrenCount = 0;
        if (parent.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
            Long c = (Long) parent.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY);
            childrenCount = c != null ? c : 0;
        } else {
            parent.addFacet(DOCUMENTS_COUNT_STATISTICS_FACET);
        }
        parent.setPropertyValue(
                DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY,
                childrenCount + count);
        session.saveDocument(parent);
    }

    protected long getCount(DocumentModel doc) throws ClientException {
        if (doc.hasFacet(FOLDERISH)) {
            if (doc.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
                Long count = (Long) doc.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY);
                return count != null ? count : 0;
            } else {
                return 0;
            }
        } else {
            return 1;
        }
    }
}
