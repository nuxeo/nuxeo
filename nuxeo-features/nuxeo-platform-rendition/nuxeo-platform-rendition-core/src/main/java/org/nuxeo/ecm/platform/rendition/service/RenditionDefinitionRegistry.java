/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.rendition.service;

import static org.apache.commons.logging.LogFactory.getLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.rendition.extension.DefaultAutomationRenditionProvider;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@link RenditionDefinition} objects.
 *
 * @since 7.3
 */
public class RenditionDefinitionRegistry extends ContributionFragmentRegistry<RenditionDefinition> {

    private static final Log log = getLog(RenditionDefinitionRegistry.class);

    protected Map<String, RenditionDefinition> descriptors = new HashMap<>();

    public RenditionDefinition getRenditionDefinition(String name) {
        RenditionDefinition renditionDefinition = descriptors.get(name);
        if (renditionDefinition == null) {
            // could be the CMIS name
            for (RenditionDefinition rd : descriptors.values()) {
                if (name.equals(rd.getCmisName())) {
                    renditionDefinition = rd;
                    break;
                }
            }
        }
        return renditionDefinition;
    }

    public List<RenditionDefinition> getRenditionDefinitions(DocumentModel doc) {
        List<RenditionDefinition> renditionDefinitions = new ArrayList<>();

        for (RenditionDefinition descriptor : descriptors.values()) {
            if (canUseRenditionDefinition(descriptor, doc) && descriptor.getProvider().isAvailable(doc, descriptor)) {
                renditionDefinitions.add(descriptor);
            }
        }

        return renditionDefinitions;
    }

    protected boolean canUseRenditionDefinition(RenditionDefinition renditionDefinition, DocumentModel doc) {
        ActionManager actionService = Framework.getService(ActionManager.class);
        return actionService.checkFilters(renditionDefinition.getFilterIds(), createActionContext(doc));
    }

    protected ActionContext createActionContext(DocumentModel doc) {
        ActionContext actionContext = new ELActionContext();
        actionContext.setCurrentDocument(doc);
        CoreSession coreSession = doc.getCoreSession();
        actionContext.setDocumentManager(coreSession);
        if (coreSession != null) {
            actionContext.setCurrentPrincipal(coreSession.getPrincipal());
        }
        return actionContext;
    }

    @Override
    public String getContributionId(RenditionDefinition renditionDefinition) {
        return renditionDefinition.getName();
    }

    @Override
    public void contributionUpdated(String id, RenditionDefinition contrib, RenditionDefinition newOrigContrib) {
        if (contrib.isEnabled()) {
            descriptors.put(id, contrib);
            setupProvider(contrib);
        } else {
            descriptors.remove(id);
        }
    }

    protected void setupProvider(RenditionDefinition definition) {
        if (definition.getProviderClass() == null) {
            definition.setProvider(new DefaultAutomationRenditionProvider());
        } else {
            try {
                RenditionProvider provider = definition.getProviderClass().getDeclaredConstructor().newInstance();
                definition.setProvider(provider);
            } catch (Exception e) {
                log.error("Unable to create RenditionProvider", e);
            }
        }
    }

    @Override
    public void contributionRemoved(String id, RenditionDefinition contrib) {
        descriptors.remove(id);
    }

    @Override
    public RenditionDefinition clone(RenditionDefinition contrib) {
        return new RenditionDefinition(contrib);
    }

    @Override
    public void merge(RenditionDefinition source, RenditionDefinition dest) {
        dest.merge(source);
    }

}
