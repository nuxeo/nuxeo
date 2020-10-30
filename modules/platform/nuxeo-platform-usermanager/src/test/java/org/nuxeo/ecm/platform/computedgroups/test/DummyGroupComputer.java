/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.computedgroups.test;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.computedgroups.AbstractGroupComputer;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

public class DummyGroupComputer extends AbstractGroupComputer {

    List<String> grpNames = new ArrayList<>();

    public DummyGroupComputer() {
        grpNames.add("Grp1");
        grpNames.add("Grp2");
    }

    @Override
    public List<String> getGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal) {

        List<String> grps = new ArrayList<>();
        if (nuxeoPrincipal.getName().contains("1")) {
            grps.add("Grp1");
        }
        if (nuxeoPrincipal.getName().contains("2")) {
            grps.add("Grp2");
        }

        return grps;
    }

    @Override
    public List<String> getGroupMembers(String groupName) {
        List<String> names = new ArrayList<>();

        if ("Grp1".equals(groupName)) {
            names.add("User1");
            names.add("User12");
        } else if ("Grp2".equals(groupName)) {
            names.add("User2");
            names.add("User12");
        }
        return names;
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
    public List<String> getAllGroupIds() {
        return grpNames;
    }

}
