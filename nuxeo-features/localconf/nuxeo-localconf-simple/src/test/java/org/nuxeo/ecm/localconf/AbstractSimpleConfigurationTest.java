/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.localconf;

import static org.nuxeo.ecm.localconf.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
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
import org.nuxeo.ecm.localconf.SimpleConfiguration;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public abstract class AbstractSimpleConfigurationTest {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected LocalConfigurationService localConfigurationService;

    public static final DocumentRef PARENT_WORKSPACE_REF = new PathRef("/default-domain/workspaces/workspace");

    public static final DocumentRef CHILD_WORKSPACE_REF = new PathRef("/default-domain/workspaces/workspace/workspace2");

    public static final DocumentRef FOLDER_REF = new PathRef("/default-domain/workspaces/workspace/a-folder");

    protected DocumentModel initializeSimpleConfiguration(DocumentModel doc, Map<String, String> parameters)
            {
        doc.addFacet(SIMPLE_CONFIGURATION_FACET);
        doc = session.saveDocument(doc);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, doc);
        simpleConfiguration.putAll(parameters);
        simpleConfiguration.save(session);

        // refetch document
        return session.getDocument(doc.getRef());
    }

    protected DocumentModel initializeSimpleConfiguration(DocumentModel doc) {
        return initializeSimpleConfiguration(doc, Collections.<String, String> emptyMap());
    }

    protected void addReadForEveryone(DocumentRef ref) {
        DocumentModel childWorkspace = session.getDocument(ref);
        ACP acp = childWorkspace.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.clear();
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ, true));
        childWorkspace.setACP(acp, true);
        session.saveDocument(childWorkspace);
        session.save();
    }

    protected CloseableCoreSession openSessionAs(String username) {
        return coreFeature.openCoreSession(username);
    }

}
