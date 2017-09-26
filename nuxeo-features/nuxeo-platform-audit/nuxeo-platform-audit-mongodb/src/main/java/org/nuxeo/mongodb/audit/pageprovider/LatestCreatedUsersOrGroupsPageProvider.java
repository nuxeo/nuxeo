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
 *     Guillaume Renard
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit.pageprovider;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 9.1
 */
public class LatestCreatedUsersOrGroupsPageProvider extends AbstractPageProvider<DocumentModel> {

    private static final long serialVersionUID = 1L;

    public final static String LATEST_CREATED_USERS_OR_GROUPS_PROVIDER = "LATEST_CREATED_USERS_OR_GROUPS_PROVIDER";

    public final static String LATEST_AUDITED_CREATED_USERS_OR_GROUPS_PROVIDER = "LATEST_AUDITED_CREATED_USERS_OR_GROUPS_PROVIDER";

    protected List<DocumentModel> currentPage;

    @Override
    public List<DocumentModel> getCurrentPage() {
        if (currentPage != null) {
            return currentPage;
        }
        currentPage = new ArrayList<>();
        PageProviderService pps = Framework.getService(PageProviderService.class);
        CoreSession coreSession = (CoreSession) getProperties().get(MongoDBAuditPageProvider.CORE_SESSION_PROPERTY);
        PageProvider<?> pp = pps.getPageProvider(LATEST_AUDITED_CREATED_USERS_OR_GROUPS_PROVIDER, null, getPageSize(),
                getCurrentPageIndex(), getProperties(),
                coreSession != null ? coreSession.getRootDocument().getId() : new Object[] { null });
        @SuppressWarnings("unchecked")
        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        if (entries != null) {
            UserManager um = Framework.getService(UserManager.class);
            for (LogEntry e : entries) {
                String id = (String) e.getExtendedInfos().get("id").getSerializableValue();
                if (StringUtils.isNotBlank(id)) {
                    DocumentModel doc;
                    if (UserManagerImpl.GROUPCREATED_EVENT_ID.equals(e.getEventId())) {
                        doc = um.getGroupModel(id);
                    } else if (UserManagerImpl.USERCREATED_EVENT_ID.equals(e.getEventId())) {
                        doc = um.getUserModel(id);
                    } else {
                        break;
                    }
                    if (doc == null) {
                        // probably user/group does not exist anymore
                        break;
                    }
                    currentPage.add(doc);
                }
            }
        }
        return currentPage;
    }

    @Override
    protected void pageChanged() {
        currentPage = null;
        super.pageChanged();
    }

}
