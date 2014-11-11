/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.impl.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
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
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        DocumentRef rootRef = session.getRootDocument().getRef();

        Filter filter = new LifeCycleFilter("deleted", false);

        for (DocumentModel doc : session.getChildren(rootRef, "Domain", filter, null)) {
            result.add(doc);
        }

        return result;
    }

    public List<DocumentModel> getDomains() throws ClientException {
        if (domains == null) {
            runUnrestricted();
        }
        return domains;
    }

}
