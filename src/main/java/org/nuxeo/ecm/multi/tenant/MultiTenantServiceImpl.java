/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_ADMINISTRATORS_GROUP_SUFFIX;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_CONFIG_FACET;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_GROUP_PREFIX;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_ID_PROPERTY;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_MEMBERS_GROUP_SUFFIX;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantServiceImpl implements MultiTenantService {

    public static final String TENANT_ACL_NAME = "tenantACP";

    private Boolean isTenantIsolationEnabled;

    @Override
    public boolean isTenantIsolationEnabled(CoreSession session)
            throws ClientException {
        if (isTenantIsolationEnabled == null) {
            final List<DocumentModel> tenants = new ArrayList<DocumentModel>();
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    String query = "SELECT * FROM Document WHERE ecm:mixinType = 'TenantConfig'";
                    tenants.addAll(session.query(query));
                }
            }.runUnrestricted();
            isTenantIsolationEnabled = !tenants.isEmpty();
        }
        return isTenantIsolationEnabled;
    }

    @Override
    public void enableTenantIsolation(CoreSession session)
            throws ClientException {
        if (!isTenantIsolationEnabled(session)) {
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    String query = "SELECT * FROM Document WHERE ecm:primaryType = 'Domain'";
                    List<DocumentModel> docs = session.query(query);
                    for (DocumentModel doc : docs) {
                        enableTenantIsolationFor(session, doc);
                    }
                    session.save();
                }
            }.runUnrestricted();
            isTenantIsolationEnabled = true;
        }
    }

    @Override
    public void disableTenantIsolation(CoreSession session)
            throws ClientException {
        if (isTenantIsolationEnabled(session)) {
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    String query = "SELECT * FROM Document WHERE ecm:mixinType = 'TenantConfig'";
                    List<DocumentModel> docs = session.query(query);
                    for (DocumentModel doc : docs) {
                        disableTenantIsolationFor(session, doc);
                    }
                    session.save();
                }
            }.runUnrestricted();
            isTenantIsolationEnabled = false;
        }
    }

    @Override
    public void enableTenantIsolationFor(CoreSession session, DocumentModel doc)
            throws ClientException {
        if (!doc.hasFacet(TENANT_CONFIG_FACET)) {
            doc.addFacet(TENANT_CONFIG_FACET);
        }
        String tenantId = (String) doc.getPropertyValue(TENANT_ID_PROPERTY);
        if (StringUtils.isBlank(tenantId)) {
            doc.setPropertyValue(TENANT_ID_PROPERTY, doc.getName());
        }
        setTenantACL(doc);
        session.saveDocument(doc);
    }

    private void setTenantACL(DocumentModel doc) throws ClientException {
        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL(TENANT_ACL_NAME);
        UserManager userManager = Framework.getLocalService(UserManager.class);
        for (String adminGroup : userManager.getAdministratorsGroups()) {
            acl.add(new ACE(adminGroup, EVERYTHING, true));
        }

        String administratorsGroup = TENANT_GROUP_PREFIX + doc.getId()
                + TENANT_ADMINISTRATORS_GROUP_SUFFIX;
        String membersGroup = TENANT_GROUP_PREFIX + doc.getId()
                + TENANT_MEMBERS_GROUP_SUFFIX;
        acl.add(new ACE(administratorsGroup, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE(membersGroup, SecurityConstants.READ_WRITE, true));
        acl.add(new ACE(EVERYONE, SecurityConstants.EVERYTHING, false));
        doc.setACP(acp, true);
    }

    @Override
    public void disableTenantIsolationFor(CoreSession session, DocumentModel doc)
            throws ClientException {
        if (doc.hasFacet(TENANT_CONFIG_FACET)) {
            doc.removeFacet(TENANT_CONFIG_FACET);
        }
        removeTenantACL(doc);
        session.saveDocument(doc);
    }

    private void removeTenantACL(DocumentModel doc) throws ClientException {
        ACP acp = doc.getACP();
        acp.removeACL(TENANT_ACL_NAME);
        doc.setACP(acp, true);
    }

    @Override
    public List<DocumentModel> getTenants(CoreSession session)
            throws ClientException {
        final List<DocumentModel> tenants = new ArrayList<DocumentModel>();
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() throws ClientException {
                String query = "SELECT * FROM Document WHERE ecm:mixinType = 'TenantConfig'";
                tenants.addAll(session.query(query));
            }
        }.runUnrestricted();
        return tenants;
    }

}
