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
