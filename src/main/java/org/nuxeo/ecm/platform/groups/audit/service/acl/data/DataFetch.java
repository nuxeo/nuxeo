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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

public class DataFetch {
    private static Log log = LogFactory.getLog(DataFetch.class);

    public static final int DEFAULT_PAGE_SIZE = 100;

    public static final boolean ORDERBY_PATH = true;

    public DocumentModelList getAllChildren(CoreSession session, DocumentModel doc) throws IOException {
        String request = getChildrenDocQuery(doc, ORDERBY_PATH);
        log.debug("start query: " + request);
        DocumentModelList res = session.query(request);
        log.debug("done query");

        return res;
    }

    public PageProvider<DocumentModel> getAllChildrenPaginated(CoreSession session, DocumentModel doc)
            {
        return getAllChildrenPaginated(session, doc, DEFAULT_PAGE_SIZE, ORDERBY_PATH);
    }

    public CoreQueryDocumentPageProvider getAllChildrenPaginated(CoreSession session, DocumentModel doc, long pageSize,
            boolean orderByPath) {
        String request = getChildrenDocQuery(doc, orderByPath);
        log.debug("will initialize a paginated query:" + request);
        PageProviderService pps = Framework.getService(PageProviderService.class);
        CoreQueryPageProviderDescriptor desc = new CoreQueryPageProviderDescriptor();
        desc.setPattern(request);

        // page provider parameters & init
        Long targetPage = null;
        Long targetPageSize = pageSize;
        List<SortInfo> sortInfos = null;
        Object[] parameters = null;
        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> provider = pps.getPageProvider("", desc, null, sortInfos, targetPageSize, targetPage, props,
                parameters);
        // TODO: edit pps implementation to really set parameters!
        provider.setPageSize(pageSize);
        provider.setMaxPageSize(pageSize);
        CoreQueryDocumentPageProvider cqdpp = (CoreQueryDocumentPageProvider) provider;
        return cqdpp;
    }

    /* QUERIES */

    public String getChildrenDocQuery(DocumentModel doc, boolean ordered) {
        String parentPath = doc.getPathAsString();

        String request = String.format("SELECT * FROM Document WHERE ecm:path STARTSWITH '%s' AND "
                + "ecm:mixinType = 'Folderish' AND %s", parentPath, baseRequest());
        if (ordered)
            return request + " ORDER BY ecm:path";
        else
            return request;
    }

    /**
     * Exclude documents:
     * <ul>
     * <li>from user workspaces
     * <li>that are deleted (in trash)
     * <li>that stand in user workspace
     * </ul>
     *
     * @return
     */
    protected static String baseRequest() {
        return "ecm:mixinType != 'HiddenInNavigation'" + " AND ecm:isCheckedInVersion = 0"
                + " AND ecm:currentLifeCycleState != 'deleted'";
    }
}
