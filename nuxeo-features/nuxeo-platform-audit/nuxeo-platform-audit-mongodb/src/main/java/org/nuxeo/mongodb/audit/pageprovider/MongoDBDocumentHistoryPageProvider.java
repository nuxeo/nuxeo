/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit.pageprovider;

import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_DOC_UUID;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_EVENT_DATE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.document.AdditionalDocumentAuditParams;
import org.nuxeo.ecm.platform.audit.api.document.DocumentAuditHelper;
import org.nuxeo.runtime.mongodb.MongoDBSerializationHelper;

/**
 * @since 9.1
 */
public class MongoDBDocumentHistoryPageProvider extends MongoDBAuditPageProvider {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(MongoDBDocumentHistoryPageProvider.class);

    public static final String SINGLE_QUERY = String.format("{ \"%s\": \"?\" }", PROPERTY_DOC_UUID);

    public static final String COMPLEX_QUERY = String.format(
            "{ \"$or\": [ { \"%s\": \"?\" }, { \"$and\": [ { \"%s\": \"?\" }, { \"%s\": { \"$lte\": ISODate(\"?\") } } ] } ]}",
            PROPERTY_DOC_UUID, PROPERTY_DOC_UUID, PROPERTY_EVENT_DATE);

    protected Object[] newParams;

    @Override
    protected String getFixedPart() {
        if (getParameters().length == 3) {
            return COMPLEX_QUERY;
        }
        return SINGLE_QUERY;
    }

    @Override
    public List<SortInfo> getSortInfos() {
        List<SortInfo> sort = super.getSortInfos();
        if (sort == null || sort.size() == 0) {
            sort = new ArrayList<>(2);
            sort.add(new SortInfo(PROPERTY_EVENT_DATE, true));
            sort.add(new SortInfo(MongoDBSerializationHelper.MONGODB_ID, true));
        }
        return sort;
    }

    @Override
    public Object[] getParameters() {
        if (newParams == null) {
            Object[] params = super.getParameters();
            if (params.length != 1) {
                log.error(this.getClass().getSimpleName()
                        + " Expect only one parameter the document uuid, unexpected behavior may occur");
            }
            CoreSession session;
            String uuid;
            if (params[0] instanceof DocumentModel) {
                DocumentModel doc = (DocumentModel) params[0];
                uuid = doc.getId();
                session = doc.getCoreSession();
            } else {
                session = (CoreSession) getProperties().get(CORE_SESSION_PROPERTY);
                uuid = params[0].toString();
            }
            if (session != null) {
                AdditionalDocumentAuditParams additionalParams = DocumentAuditHelper.getAuditParamsForUUID(uuid,
                        session);
                if (additionalParams != null) {
                    newParams = new Object[] { uuid, additionalParams.getTargetUUID(), additionalParams.getMaxDate() };
                } else {
                    newParams = new Object[] { uuid };
                }
            } else {
                log.warn("No core session found: cannot compute all info to get complete audit entries");
                return params;
            }
        }
        return newParams;
    }

    @Override
    public boolean hasChangedParameters(Object[] parameters) {
        return getParametersChanged(this.parameters, parameters);
    }

}
