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

    public Multimap<String, Pair<String, Boolean>> getUserAcls() {
        return userAcls;
    }

    protected String title;

    protected String path;

    protected int depth;

    protected boolean aclLockInheritance;

    protected Multimap<String, Pair<String, Boolean>> userAcls;
}
