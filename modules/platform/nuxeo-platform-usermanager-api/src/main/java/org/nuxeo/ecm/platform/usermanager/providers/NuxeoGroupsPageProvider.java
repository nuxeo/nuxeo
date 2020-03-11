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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.usermanager.providers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Page provider listing {@link NuxeoGroup}s.
 *
 * @since 5.8
 */
public class NuxeoGroupsPageProvider extends AbstractGroupsPageProvider<NuxeoGroup> {

    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(NuxeoGroupsPageProvider.class);

    @Override
    public List<NuxeoGroup> getCurrentPage() {
        List<DocumentModel> groups = computeCurrentPage();
        List<NuxeoGroup> nuxeoGroups = new ArrayList<>(groups.size());
        UserManager userManager = Framework.getService(UserManager.class);
        for (DocumentModel group : groups) {
            NuxeoGroup nuxeoGroup = userManager.getGroup(
                    group.getProperty(userManager.getGroupIdField()).getValue(String.class));
            nuxeoGroups.add(nuxeoGroup);
        }
        return nuxeoGroups;
    }
}
