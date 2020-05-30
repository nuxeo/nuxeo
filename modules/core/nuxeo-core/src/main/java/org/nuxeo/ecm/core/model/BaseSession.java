/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.model;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.runtime.api.Framework;

/**
 * Common code for VCS and DBS {@link Session} implementations.
 *
 * @since 11.3
 */
public abstract class BaseSession implements Session<QueryFilter> {

    protected final Repository repository;

    protected BaseSession(Repository repository) {
        this.repository = repository;
    }

    protected DocumentBlobManager getDocumentBlobManager() {
        return Framework.getService(DocumentBlobManager.class);
    }

    protected void notifyAfterCopy(Document doc) {
        getDocumentBlobManager().notifyAfterCopy(doc);
    }

    /*
     * ----- Common ACP code -----
     */

    protected void checkNegativeAcl(ACP acp) {
        if (acp == null || isNegativeAclAllowed()) {
            return;
        }
        for (ACL acl : acp.getACLs()) {
            if (acl.getName().equals(ACL.INHERITED_ACL)) {
                continue;
            }
            for (ACE ace : acl.getACEs()) {
                if (ace.isGranted()) {
                    continue;
                }
                String permission = ace.getPermission();
                if (permission.equals(SecurityConstants.EVERYTHING)
                        && ace.getUsername().equals(SecurityConstants.EVERYONE)) {
                    continue;
                }
                // allow Write, as we're sure it doesn't include Read/Browse
                if (permission.equals(SecurityConstants.WRITE)) {
                    continue;
                }
                throw new IllegalArgumentException("Negative ACL not allowed: " + ace);
            }
        }
    }

    /**
     * Gets the ACP for the document (without any inheritance).
     *
     * @param doc the document
     * @return the ACP
     */
    public abstract ACP getACP(Document doc);

    @Override
    public ACP getMergedACP(Document doc) {
        Document base = doc.isVersion() ? doc.getSourceDocument() : doc;
        if (base == null) {
            return null;
        }
        ACP acp = getACP(base);
        if (doc.getParent() == null) {
            return acp;
        }
        // get inherited ACLs only if no blocking inheritance ACE exists in the top level ACP
        ACL acl = null;
        if (acp == null || acp.getAccess(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING) != Access.DENY) {
            acl = getInheritedACLs(doc);
        }
        if (acp == null) {
            if (acl == null) {
                return null;
            }
            acp = new ACPImpl();
        }
        if (acl != null) {
            acp.addACL(acl);
        }
        return acp;
    }

    protected ACL getInheritedACLs(Document doc) {
        doc = doc.getParent();
        ACL merged = null;
        while (doc != null) {
            ACP acp = getACP(doc);
            if (acp != null) {
                ACL acl = acp.getMergedACLs(ACL.INHERITED_ACL);
                if (merged == null) {
                    merged = acl;
                } else {
                    merged.addAll(acl);
                }
                if (acp.getAccess(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING) == Access.DENY) {
                    break;
                }
            }
            doc = doc.getParent();
        }
        return merged;
    }

    /**
     * Returns the merge of two ACPs.
     */
    protected ACP updateACP(ACP curAcp, ACP addAcp) {
        if (curAcp == null) {
            return addAcp;
        }
        ACP newAcp = curAcp.clone(); // clone as we may modify ACLs and ACPs
        Map<String, ACL> acls = new HashMap<>();
        for (ACL acl : newAcp.getACLs()) {
            String name = acl.getName();
            if (ACL.INHERITED_ACL.equals(name)) {
                throw new IllegalStateException(curAcp.toString());
            }
            acls.put(name, acl);
        }
        for (ACL acl : addAcp.getACLs()) {
            String name = acl.getName();
            if (ACL.INHERITED_ACL.equals(name)) {
                continue;
            }
            ACL curAcl = acls.get(name);
            if (curAcl != null) {
                // TODO avoid duplicates
                curAcl.addAll(acl);
            } else {
                newAcp.addACL(acl);
            }
        }
        return newAcp;
    }

}
