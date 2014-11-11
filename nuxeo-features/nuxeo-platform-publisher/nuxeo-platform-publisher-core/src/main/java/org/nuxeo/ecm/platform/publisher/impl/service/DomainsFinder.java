/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.publisher.impl.service;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * Finds the domains for a session.
 */
public class DomainsFinder extends UnrestrictedSessionRunner {

    protected List<DocumentModel> domains;

    public DomainsFinder(String repositoryName) {
        super(repositoryName);
    }

    @Override
    public void run() throws ClientException {
        domains = getDomainsFiltered();
    }

    protected List<DocumentModel> getDomainsFiltered() throws ClientException {
        String query = "SELECT * FROM Document WHERE ecm:primaryType = 'Domain' AND "
                + NXQL.ECM_PARENTID
                + " = '%s' AND "
                + NXQL.ECM_LIFECYCLESTATE
                + " <> '" + LifeCycleConstants.DELETED_STATE + "'";
        query = String.format(query, session.getRootDocument().getId());
        return session.query(query); // should disconnect
    }

    public List<DocumentModel> getDomains() throws ClientException {
        if (domains == null) {
            runUnrestricted();
        }
        return domains;
    }

}
