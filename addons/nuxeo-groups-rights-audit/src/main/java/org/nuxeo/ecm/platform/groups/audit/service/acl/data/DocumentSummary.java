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

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

import org.nuxeo.ecm.platform.groups.audit.service.acl.Pair;

import com.google.common.collect.Multimap;

public class DocumentSummary {
    public DocumentSummary(String title, int depth, boolean aclLockInheritance,
            Multimap<String, Pair<String, Boolean>> userAcls) {
        this.title = title;
        this.depth = depth;
        this.aclLockInheritance = aclLockInheritance;
        this.userAcls = userAcls;
    }

    public DocumentSummary(String title, int depth, boolean aclLockInheritance,
            Multimap<String, Pair<String, Boolean>> aclLocal, Multimap<String, Pair<String, Boolean>> aclInherited,
            String path) {
        this.title = title;
        this.depth = depth;
        this.aclLockInheritance = aclLockInheritance;
        this.userAcls = aclLocal;
        this.userAclsInherited = aclInherited;
        this.path = path;
    }

    public DocumentSummary(String title, int depth, boolean aclLockInheritance,
            Multimap<String, Pair<String, Boolean>> userAcls, String path) {
        this.title = title;
        this.depth = depth;
        this.aclLockInheritance = aclLockInheritance;
        this.userAcls = userAcls;
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public int getDepth() {
        return depth;
    }

    public String getPath() {
        return path;
    }

    public boolean isAclLockInheritance() {
        return aclLockInheritance;
    }

    public Multimap<String, Pair<String, Boolean>> getAclByUser() {
        return userAcls;
    }

    public Multimap<String, Pair<String, Boolean>> getAclInheritedByUser() {
        return userAclsInherited;
    }

    public void setAclInheritedByUser(Multimap<String, Pair<String, Boolean>> userAclsInherited) {
        this.userAclsInherited = userAclsInherited;
    }

    protected String title;

    protected String path;

    protected int depth;

    protected boolean aclLockInheritance;

    protected Multimap<String, Pair<String, Boolean>> userAcls;

    /** If we want to have different colors for inherited ACL, we can use this structure. */
    protected Multimap<String, Pair<String, Boolean>> userAclsInherited;
}
