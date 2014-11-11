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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Finds the domains for a session.
 */
public class DomainsFinder extends UnrestrictedSessionRunner {

    private static final Log log = LogFactory.getLog(DomainsFinder.class);

    public static final String PUBLISHING_DOMAINS_PROVIDER = "domains_for_publishing";

    protected List<DocumentModel> domains;

    public DomainsFinder(String repositoryName) {
        super(repositoryName);
    }

    @Override
    public void run() throws ClientException {
        domains = getDomainsFiltered();
    }

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> getDomainsFiltered() throws ClientException {
        PageProviderService pps;
        try {
            pps = Framework.getService(PageProviderService.class);
        } catch (Exception e) {
            log.error("Failed to get PageProviderService", e);
            return new ArrayList<DocumentModel>();
        }
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) pps.getPageProvider(
                PUBLISHING_DOMAINS_PROVIDER, null, null, null, props,
                new Object[] { session.getRootDocument().getId() });
        return pageProvider.getCurrentPage();
    }

    public List<DocumentModel> getDomains() throws ClientException {
        if (domains == null) {
            runUnrestricted();
        }
        return domains;
    }

}
