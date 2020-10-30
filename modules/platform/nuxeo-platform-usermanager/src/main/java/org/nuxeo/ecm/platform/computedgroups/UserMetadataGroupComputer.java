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
 *     Benjamin Jalon
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

/**
 * Configurable Group Computer based on metadata of the user.
 *
 * @since 5.7.3
 */
public class UserMetadataGroupComputer extends AbstractGroupComputer {

    public static final Log log = LogFactory.getLog(UserMetadataGroupComputer.class);

    private String groupPattern;

    private String xpath;

    public UserMetadataGroupComputer(String xpath, String groupPattern) {
        this.xpath = xpath;
        this.groupPattern = groupPattern;

        if (xpath == null || xpath.isEmpty() || groupPattern == null || groupPattern.isEmpty()) {
            throw new NuxeoException("Bad configuration");
        }

    }

    @Override
    public List<String> getAllGroupIds() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getGroupMembers(String groupId) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getGroupsForUser(NuxeoPrincipalImpl user) {
        String value = (String) user.getModel().getPropertyValue(xpath);

        if (value == null || "".equals(value.trim())) {
            return new ArrayList<>();
        }

        ArrayList<String> result = new ArrayList<>();
        result.add(String.format(groupPattern, value));

        return result;
    }

    @Override
    public List<String> getParentsGroupNames(String arg0) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getSubGroupsNames(String arg0) {
        return new ArrayList<>();
    }

    @Override
    public boolean hasGroup(String groupId) {
        return false;
    }
}
