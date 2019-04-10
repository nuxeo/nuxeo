/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
