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
 *     Vladimir Pasquier
 */
package org.nuxeo.ecm.platform.groups.audit.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.groups.audit.service.ExcelExportFactory;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class ExcelExportAllGroups implements ExcelExportFactory {

    public static final Log log = LogFactory.getLog(ExcelExportAllGroups.class);

    @Override
    public Map<String, Object> getDataToInject() {
        UserManager userManager = Framework.getService(UserManager.class);
        List<String> groupsId = new ArrayList<>();
        List<NuxeoGroup> groups = new ArrayList<>();
        groupsId = userManager.getGroupIds();
        for (String groupId : groupsId) {
            NuxeoGroup group;
            group = userManager.getGroup(groupId);
            groups.add(group);
        }
        Map<String, Object> beans = new HashMap<>();
        beans.put("groups", groups);
        beans.put("userManager", userManager);
        return beans;
    }

}
