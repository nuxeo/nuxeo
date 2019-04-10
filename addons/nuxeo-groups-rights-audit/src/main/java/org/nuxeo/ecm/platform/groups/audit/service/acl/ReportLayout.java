/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl;

import java.util.HashMap;
import java.util.Map;

public class ReportLayout {
    protected Map<String, Integer> userColumn;

    protected Map<Pair<String, String>, Integer> userAclColumn;

    public ReportLayout() {
        reset();
    }

    public void reset() {
        userColumn = new HashMap<String, Integer>();
        userAclColumn = new HashMap<Pair<String, String>, Integer>();
    }

    /** Store the user column */
    public void setUserColumn(int column, String userOrGroup) {
        userColumn.put(userOrGroup, column);
    }

    /** Return the user column */
    public int getUserColumn(String user) {
        return userColumn.get(user);
    }

    public void setUserAclColumn(int column, Pair<String, String> userAcl) {
        userAclColumn.put(userAcl, column);
    }

    /** Return the user column */
    public int getUserAclColumn(Pair<String, String> userAcl) {
        return userAclColumn.get(userAcl);
    }
}
