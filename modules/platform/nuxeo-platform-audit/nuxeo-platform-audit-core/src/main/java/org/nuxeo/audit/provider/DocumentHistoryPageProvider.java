/*
 * (C) Copyright 2017-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.provider;

import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_DATE;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.api.document.AdditionalDocumentAuditParams;
import org.nuxeo.audit.api.document.DocumentAuditHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;

/**
 * @since 9.1
 */
public class DocumentHistoryPageProvider extends AuditPageProvider {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(DocumentHistoryPageProvider.class);

    // docUUID = ?
    protected static final String SINGLE_QUERY = "%s = ?".formatted(LOG_DOC_UUID);

    // docUUID = ? OR (docUUID = ? AND eventDate <= ?)
    protected static final String COMPLEX_QUERY = "%s = ? OR (%s = ? AND %s <= ?)".formatted(LOG_DOC_UUID, LOG_DOC_UUID,
            LOG_EVENT_DATE);

    protected Object[] enhancedParameters;

    @Override
    protected String remapFixedPart(String fixedPart) {
        if (getParameters().length == 3) {
            return COMPLEX_QUERY;
        }
        return SINGLE_QUERY;
    }

    @Override
    public List<SortInfo> getSortInfos() {
        var sort = super.getSortInfos();
        if (CollectionUtils.isEmpty(sort)) {
            sort = List.of(new SortInfo(LOG_EVENT_DATE, false), new SortInfo(LOG_DOC_UUID, false));
        }
        return sort;
    }

    @Override
    public Object[] getParameters() {
        if (enhancedParameters == null) {
            Object[] params = super.getParameters();
            if (params.length != 1) {
                log.error("Only one parameter is expected, the document uuid, unexpected behavior may occur");
            }
            CoreSession session;
            String uuid;
            if (params[0] instanceof DocumentModel doc) {
                uuid = doc.getId();
                session = doc.getCoreSession();
            } else {
                session = (CoreSession) getProperties().get(PageProviderSpec.CORE_SESSION_PROPERTY);
                uuid = params[0].toString();
            }
            if (session != null) {
                AdditionalDocumentAuditParams additionalParams = DocumentAuditHelper.getAuditParamsForUUID(uuid,
                        session);
                if (additionalParams != null) {
                    enhancedParameters = new Object[] { uuid, additionalParams.getTargetUUID(),
                            additionalParams.getMaxDate() };
                } else {
                    enhancedParameters = new Object[] { uuid };
                }
            } else {
                log.warn("No core session found: cannot compute all info to get complete audit entries");
                return params;
            }
        }
        return enhancedParameters;
    }

    @Override
    public boolean hasChangedParameters(Object[] parameters) {
        return getParametersChanged(this.parameters, parameters);
    }

}
