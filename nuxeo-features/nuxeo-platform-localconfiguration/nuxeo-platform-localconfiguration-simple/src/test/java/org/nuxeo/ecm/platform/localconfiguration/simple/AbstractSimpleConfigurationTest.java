/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.localconfiguration.simple;

import static org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;

import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public abstract class AbstractSimpleConfigurationTest {

    @Inject
    protected FeaturesRunner featuresRunner;

    @Inject
    protected CoreSession session;

    @Inject
    protected LocalConfigurationService localConfigurationService;

    public static final DocumentRef PARENT_WORKSPACE_REF = new PathRef(
            "/default-domain/workspaces/workspace");

    public static final DocumentRef CHILD_WORKSPACE_REF = new PathRef(
            "/default-domain/workspaces/workspace/workspace2");

    public static final DocumentRef FOLDER_REF = new PathRef(
            "/default-domain/workspaces/workspace/a-folder");

    protected DocumentModel initializeSimpleConfiguration(DocumentModel doc,
            Map<String, String> parameters) throws ClientException {
        doc.addFacet(SIMPLE_CONFIGURATION_FACET);
        doc = session.saveDocument(doc);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET, doc);
        simpleConfiguration.putAll(parameters);
        simpleConfiguration.save(session);

        // refetch document
        return session.getDocument(doc.getRef());
    }

    protected DocumentModel initializeSimpleConfiguration(DocumentModel doc)
            throws ClientException {
        return initializeSimpleConfiguration(doc,
                Collections.<String, String> emptyMap());
    }

    protected void addReadForEveryone(DocumentRef ref) throws ClientException {
        DocumentModel childWorkspace = session.getDocument(ref);
        ACP acp = childWorkspace.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.clear();
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ,
                true));
        childWorkspace.setACP(acp, true);
        session.saveDocument(childWorkspace);
        session.save();
    }

    protected CoreSession openSessionAs(String username) throws ClientException {
        CoreFeature coreFeature = featuresRunner.getFeature(CoreFeature.class);
        return coreFeature.getRepository().getRepositoryHandler().openSessionAs(
                username);
    }

}
