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
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Common code for VCS and DBS {@link Session} implementations.
 *
 * @since 11.3
 */
public abstract class BaseSession implements Session<QueryFilter> {

    private static final Logger log = LogManager.getLogger(BaseSession.class);

    /**
     * Configuration property controlling whether ACLs on versions are disabled.
     *
     * @since 11.3
     */
    public static final String VERSION_ACL_DISABLED_PROP = "org.nuxeo.version.acl.disabled";

    /**
     * Configuration property controlling whether ReadVersion permission is disabled.
     *
     * @since 11.3
     */
    public static final String READ_VERSION_PERM_DISABLED_PROP = "org.nuxeo.version.readversion.disabled";

    /** INTERNAL. How we deal with ACLs on versions. */
    public enum VersionAclMode {
        /** Version ACL enabled. */
        ENABLED,
        /** Version ACL disabled. */
        DISABLED,
        /** Version ACL disabled for direct access but enabled for queries. */
        LEGACY;

        public static VersionAclMode getConfiguration() {
            if (!Framework.isInitialized()) {
                // unit tests
                return ENABLED;
            }
            ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
            String val = configurationService.getString(VERSION_ACL_DISABLED_PROP).orElse("false");
            switch (val) {
            case "false":
                return ENABLED;
            case "true":
                return DISABLED;
            case "legacy":
                return LEGACY;
            default:
                log.error("Invalid value for configuration property {}: '{}'", VERSION_ACL_DISABLED_PROP, val);
                return ENABLED;
            }
        }
    }

    public static boolean isReadVersionPermissionDisabled() {
        if (!Framework.isInitialized()) {
            // unit tests
            return false;
        }
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        return configurationService.isBooleanTrue(READ_VERSION_PERM_DISABLED_PROP);
    }

    protected final Repository repository;

    protected final VersionAclMode versionAclMode;

    protected final boolean disableReadVersionPermission;

    protected BaseSession(Repository repository) {
        this.repository = repository;
        versionAclMode = VersionAclMode.getConfiguration();
        disableReadVersionPermission = isReadVersionPermissionDisabled();
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
     * Can undeletable documents be deleted. Undeletable documents are documents under legal hold or retention.They are
     * deletable only if on of these conditions is true:
     * <ul>
     * <li>{@link Session#PROP_ALLOW_DELETE_UNDELETABLE_DOCUMENTS} is true for unit tests purpose</li>
     * <li>Retention is active in governance mode and the principal is member of
     * {@link SecurityConstants#RECORDS_CLEANER_GROUP}</li>
     * </ul>
     *
     * @param principal the Nuxeo principal
     * @return {@code true} if undeletable documents can be deleted, {@code false} otherwise
     * @since 11.5
     */
    public static boolean canDeleteUndeletable(NuxeoPrincipal principal) {
        if (Framework.isBooleanPropertyTrue(PROP_ALLOW_DELETE_UNDELETABLE_DOCUMENTS)) {
            return true;
        }
        if (principal == null) { // happens in some unit tests
            return false;
        }
        boolean governanceMode = !Framework.isBooleanPropertyTrue(PROP_RETENTION_COMPLIANCE_MODE_ENABLED);
        boolean recordCleaner = principal.getGroups().contains(SecurityConstants.RECORDS_CLEANER_GROUP);
        return governanceMode && recordCleaner;
    }

    /**
     * Gets the ACP for the document (without any inheritance).
     *
     * @param doc the document
     * @return the ACP
     */
    public abstract ACP getACP(Document doc);

    protected ACP getACP(Document doc, boolean replaceReadVersionPermission) {
        ACP acp = getACP(doc);
        if (acp != null && replaceReadVersionPermission) {
            // if ReadVersion is present in an ACE, turn it into a Read
            acp.replacePermission(SecurityConstants.READ_VERSION, SecurityConstants.READ);
        }
        return acp;
    }

    @Override
    public ACP getMergedACP(Document doc) {
        boolean replaceReadVersionPermission = false;
        if (doc.isVersion()) {
            replaceReadVersionPermission = !disableReadVersionPermission;
            if (versionAclMode != VersionAclMode.ENABLED) {
                doc = doc.getSourceDocument();
                if (doc == null) {
                    // version with no live doc
                    return null;
                }
            }
        }
        ACP acp = getACP(doc, replaceReadVersionPermission);
        ACP mergedAcp = acp;
        ACL inherited = new ACLImpl(ACL.INHERITED_ACL, true); // collected inherited ACEs
        for (;;) {
            if (acp != null && acp.getAccess(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING) == Access.DENY) {
                // blocking, no need to continue
                break;
            }
            if (doc.isVersion()) {
                replaceReadVersionPermission = !disableReadVersionPermission;
                doc = doc.getSourceDocument();
            } else {
                doc = doc.getParent();
            }
            if (doc == null) {
                // can't go up
                break;
            }
            // collect inherited ACEs for this level
            acp = getACP(doc, replaceReadVersionPermission);
            if (acp != null) {
                inherited.addAll(acp.getMergedACLs(ACL.INHERITED_ACL));
            }
        }
        if (!inherited.isEmpty()) {
            if (mergedAcp == null) {
                mergedAcp = new ACPImpl();
            }
            mergedAcp.addACL(inherited);
        }
        return mergedAcp;
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
