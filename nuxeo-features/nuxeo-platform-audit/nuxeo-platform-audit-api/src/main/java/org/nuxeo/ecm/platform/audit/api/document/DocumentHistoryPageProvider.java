/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.audit.api.document;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.AuditPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * Page provider that is dedicated to fetching history of a Document.
 * <p>
 * Because of the way the Audit log is stored (i.e. mainly stores events
 * related to the live document), retrieving history of a version or of a proxy
 * requires some additional processing.
 * <p>
 * This {@link PageProvider} does not accept a fixed part in the whereclause
 * because it is automatically build by the provider itself. This
 * {@link PageProvider} expect to have :
 * <ul>
 * <li>DocumentModel or UUID as input parameter</li>
 * <li>CoreSession as property (only used if input parameter is an uuid)</li>
 * </ul>
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class DocumentHistoryPageProvider extends AuditPageProvider {

    private static final long serialVersionUID = 1L;

    protected Log log = LogFactory.getLog(DocumentHistoryPageProvider.class);

    protected Object[] newParams;

    @Override
    protected String getFixedPart() {
        if (getParameters().length == 3) {
            return " ( log.docUUID = ? OR (log.docUUID = ? AND log.eventDate <= ?) ) ";
        } else {
            return " log.docUUID = ?  ";
        }
    }

    @Override
    protected boolean allowSimplePattern() {
        return false;
    }

    @Override
    public List<SortInfo> getSortInfos() {

        List<SortInfo> sort = super.getSortInfos();
        if (sort == null || sort.size() == 0) {
            sort = new ArrayList<SortInfo>();
            sort.add(new SortInfo("log.eventDate", true));
            sort.add(new SortInfo("log.id", true));
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
            CoreSession session = null;
            String uuid = null;
            if (params[0] instanceof DocumentModel) {
                DocumentModel doc = (DocumentModel) params[0];
                uuid = doc.getId();
                session = doc.getCoreSession();
            } else {
                session = (CoreSession) getProperties().get(
                        CORE_SESSION_PROPERTY);
                uuid = params[0].toString();
            }
            if (session != null) {
                try {
                    AdditionalDocumentAuditParams additionalParams = DocumentAuditHelper.getAuditParamsForUUID(
                            uuid, session);
                    if (additionalParams != null) {
                        newParams = new Object[] { uuid,
                                additionalParams.targetUUID,
                                additionalParams.maxDate };
                    } else {
                        newParams = new Object[] { uuid };
                    }
                } catch (Exception e) {
                    log.error(
                            "Error while fetching additional parameters for audit query",
                            e);
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
