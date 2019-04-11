/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.publisher.impl.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static final String PUBLISHING_DOMAINS_PROVIDER = "domains_for_publishing";

    protected List<DocumentModel> domains;

    public DomainsFinder(String repositoryName) {
        super(repositoryName);
    }

    @Override
    public void run() {
        domains = getDomainsFiltered();
        domains.forEach(domain -> domain.detach(true));
    }

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> getDomainsFiltered() {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) pps.getPageProvider(
                PUBLISHING_DOMAINS_PROVIDER, null, null, null, props,
                new Object[] { session.getRootDocument().getId() });
        return pageProvider.getCurrentPage();
    }

    public List<DocumentModel> getDomains() {
        if (domains == null) {
            runUnrestricted();
        }
        return domains;
    }

}
