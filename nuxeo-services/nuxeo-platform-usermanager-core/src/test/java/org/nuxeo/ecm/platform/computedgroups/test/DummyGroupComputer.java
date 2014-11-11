/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.computedgroups.test;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.computedgroups.AbstractGroupComputer;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

public class DummyGroupComputer extends AbstractGroupComputer {

    List<String> grpNames = new ArrayList<String>();

    public DummyGroupComputer() {
        grpNames.add("Grp1");
        grpNames.add("Grp2");
    }

    public List<String> getGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal)
            throws Exception {

        List<String> grps = new ArrayList<String>();
        if (nuxeoPrincipal.getName().contains("1")) {
            grps.add("Grp1");
        }
        if (nuxeoPrincipal.getName().contains("2")) {
            grps.add("Grp2");
        }

        return grps;
    }

    public List<String> getGroupMembers(String groupName) throws Exception {
        List<String> names = new ArrayList<String>();

        if ("Grp1".equals(groupName)) {
            names.add("User1");
            names.add("User12");
        } else if ("Grp2".equals(groupName)) {
            names.add("User2");
            names.add("User12");
        }
        return names;
    }

    public List<String> getParentsGroupNames(String groupName) throws Exception {
        return null;
    }

    public List<String> getSubGroupsNames(String groupName) throws Exception {
        return null;
    }

    public List<String> getAllGroupIds() throws Exception {
        return grpNames;
    }

}
