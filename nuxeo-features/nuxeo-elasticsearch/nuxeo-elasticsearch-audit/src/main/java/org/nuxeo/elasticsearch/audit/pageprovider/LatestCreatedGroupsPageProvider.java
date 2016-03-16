/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "Guillaume Renard"
 */

package org.nuxeo.elasticsearch.audit.pageprovider;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.1
 */
public class LatestCreatedGroupsPageProvider extends AbstractPageProvider<NuxeoGroup> {

    private static final long serialVersionUID = 1L;

    public final static String LATEST_CREATED_GROUPS_PROVIDER = "LATEST_CREATED_GROUPS_PROVIDER";

    public final static String LATEST_AUDITED_GROUPS_USERS_PROVIDER = "LATEST_AUDITED_CREATED_GROUPS_PROVIDER";

    @Override
    public List<NuxeoGroup> getCurrentPage() {

        List<NuxeoGroup> result = new ArrayList<NuxeoGroup>();
        PageProviderService pps = Framework.getService(PageProviderService.class);
        UserManager um = Framework.getService(UserManager.class);
        PageProvider<?> pp = pps.getPageProvider("LATEST_AUDITED_CREATED_GROUPS_PROVIDER", null, getPageSize(),
                getCurrentPageIndex(), getProperties(), getParameters());
        @SuppressWarnings("unchecked")
        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        if (entries != null) {
            for (LogEntry e : entries) {
                String groupId = (String) e.getExtendedInfos().get("id").getSerializableValue();
                if (StringUtils.isNotBlank(groupId)) {
                    NuxeoGroup np = um.getGroup(groupId);
                    if (np == null) {
                        np = new NuxeoGroupImpl(groupId);
                    }
                    result.add(np);
                }
            }
        }
        return result;
    }

}
