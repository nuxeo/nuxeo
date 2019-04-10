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

import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY;
import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.quota.QuotaStatsNonFolderishCount;

/**
 * Adapter implementing {@code QuotaStatsNonFolderishCount} to have information about children and descendants count.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class QuotaStatsNonFolderishCountAdapter implements QuotaStatsNonFolderishCount {

    private final DocumentModel doc;

    public QuotaStatsNonFolderishCountAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public long getIntrinsic() {
        return 0;
    }

    @Override
    public long getChildren() {
        try {
            Long count = (Long) doc.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_CHILDREN_COUNT_PROPERTY);
            return count != null ? count : 0;
        } catch (PropertyException e) {
            return 0;
        }
    }

    @Override
    public long getTotal() {
        try {
            Long count = (Long) doc.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY);
            return count != null ? count : 0;
        } catch (PropertyException e) {
            return 0;
        }
    }

}
