/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

/**
 *
 * Base class for {@link GroupComputer} implemenytation that uses User attribute to compute groups
 *
 * @author Thierry Delprat
 *
 */
public abstract class AbstractAttributeBasedGroupComputer extends
        AbstractGroupComputer implements GroupComputer {


    protected abstract String getAttributeForGroupComputation();

    public List<String> getAllGroupIds() throws Exception {

        List<String> companies = new ArrayList<String>();
        for (String userId : getUM().getUserIds()) {
            NuxeoPrincipal principal = getUM().getPrincipal(userId);
            String companyName = principal.getCompany();
            if (!companies.contains(companyName)) {
                companies.add(companyName);
            }
        }
        return companies;
    }

    public List<String> getGroupMembers(String groupName) throws Exception {

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put(getAttributeForGroupComputation(), groupName);

        DocumentModelList users = getUM().searchUsers(filter, null);

        List<String> memberIds = new ArrayList<String>();

        for (DocumentModel user : users) {
            memberIds.add(user.getId());
        }
        return memberIds;
    }

    public List<String> getGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal)
            throws Exception {
        List<String> grpNames = new ArrayList<String>();
        String property = (String) nuxeoPrincipal.getModel().getProperty(getUM().getUserSchemaName(), getAttributeForGroupComputation());
        if (property!=null && ! "".equals(property.trim())) {
            grpNames.add(property);
        }
        return grpNames;
    }

    public List<String> getParentsGroupNames(String groupName) throws Exception {
        return null;
    }

    public List<String> getSubGroupsNames(String groupName) throws Exception {
        return null;
    }

    public List<String> searchGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) throws Exception {

        List<String> companies = new ArrayList<String>();

        String grpName = (String) filter.get(getUM().getGroupIdField());
        if (grpName != null) {
            Map<String, Serializable> gFilter = new HashMap<String, Serializable>();
            HashSet<String> gFulltext = new HashSet<String>();
            gFilter.put(getAttributeForGroupComputation(), grpName);
            gFulltext.add(getAttributeForGroupComputation());
            for (DocumentModel userDoc : getUM()
                    .searchUsers(gFilter, gFulltext)) {
                String companyName = (String) userDoc.getProperty(getUM()
                        .getUserSchemaName(), getAttributeForGroupComputation());
                if (!companies.contains(companyName)) {
                    companies.add(companyName);
                }
            }
        }
        return companies;
    }
}
