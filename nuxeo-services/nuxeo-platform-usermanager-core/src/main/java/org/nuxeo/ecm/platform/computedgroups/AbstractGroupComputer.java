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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for {@link GroupComputer} implementation. Provides a naive
 * implementation for searchGroups method.
 *
 * @author Thierry Delprat
 *
 */
public abstract class AbstractGroupComputer implements GroupComputer {

    protected UserManager getUM() {
        return Framework.getLocalService(UserManager.class);
    }

    /**
     * Default implementation that searches on all ids for a match.
     */
    public List<String> searchGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) throws Exception {

        List<String> result = new ArrayList<String>();
        String grpName = (String) filter.get(getUM().getGroupIdField());
        if (grpName != null) {
            List<String> allGroupIds = getAllGroupIds();
            if (allGroupIds != null) {
                for (String vGroupName : allGroupIds) {
                    if (vGroupName.startsWith(grpName)) {
                        if (!result.contains(vGroupName)) {
                            result.add(vGroupName);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Default implementation that returns true if method
     * {@link GroupComputer#getAllGroupIds()} contains given group name.
     */
    public boolean hasGroup(String name) throws Exception {
        List<String> allGroupIds = getAllGroupIds();
        if (allGroupIds != null) {
            return allGroupIds.contains(name);
        }
        return false;
    }

}
