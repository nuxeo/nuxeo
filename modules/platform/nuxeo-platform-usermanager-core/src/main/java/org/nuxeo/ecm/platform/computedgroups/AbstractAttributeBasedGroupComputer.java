/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 * *
 */
package org.nuxeo.ecm.platform.computedgroups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

/**
 * Base class for {@link GroupComputer} implementation that uses User attribute to compute groups.
 *
 * @author Thierry Delprat
 */
public abstract class AbstractAttributeBasedGroupComputer extends AbstractGroupComputer {

    protected abstract String getAttributeForGroupComputation();

    @Override
    public List<String> getAllGroupIds() {

        List<String> companies = new ArrayList<>();
        for (String userId : getUM().getUserIds()) {
            DocumentModel doc = getUM().getUserModel(userId);
            if (doc != null) {
                String companyName = (String) doc.getProperty(getUM().getUserSchemaName(),
                        getAttributeForGroupComputation());
                if (!companies.contains(companyName)) {
                    companies.add(companyName);
                }
            }
        }
        return companies;
    }

    @Override
    public List<String> getGroupMembers(String groupName) {

        Map<String, Serializable> filter = new HashMap<>();
        filter.put(getAttributeForGroupComputation(), groupName);

        DocumentModelList users = getUM().searchUsers(filter, null);

        List<String> memberIds = new ArrayList<>();

        for (DocumentModel user : users) {
            memberIds.add(user.getId());
        }
        return memberIds;
    }

    @Override
    public List<String> getGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal) {
        List<String> grpNames = new ArrayList<>();
        String property = (String) nuxeoPrincipal.getModel().getProperty(getUM().getUserSchemaName(),
                getAttributeForGroupComputation());
        if (property != null && !"".equals(property.trim())) {
            grpNames.add(property);
        }
        return grpNames;
    }

    @Override
    public List<String> getParentsGroupNames(String groupName) {
        return null;
    }

    @Override
    public List<String> getSubGroupsNames(String groupName) {
        return null;
    }

    @Override
    public List<String> searchGroups(Map<String, Serializable> filter, Set<String> fulltext) {

        List<String> companies = new ArrayList<>();

        String grpName = (String) filter.get(getUM().getGroupIdField());
        if (grpName != null) {
            Map<String, Serializable> gFilter = new HashMap<>();
            Set<String> gFulltext = new HashSet<>();
            gFilter.put(getAttributeForGroupComputation(), grpName);
            gFulltext.add(getAttributeForGroupComputation());
            for (DocumentModel userDoc : getUM().searchUsers(gFilter, gFulltext)) {
                String companyName = (String) userDoc.getProperty(getUM().getUserSchemaName(),
                        getAttributeForGroupComputation());
                if (!companies.contains(companyName)) {
                    companies.add(companyName);
                }
            }
        }
        return companies;
    }
}
