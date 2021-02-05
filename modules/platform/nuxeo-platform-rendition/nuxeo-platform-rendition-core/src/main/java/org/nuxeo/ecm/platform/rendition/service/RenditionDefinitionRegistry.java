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

import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.rendition.extension.DefaultAutomationRenditionProvider;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Registry for {@link RenditionDefinition} objects.
 *
 * @since 7.3
 */
public class RenditionDefinitionRegistry extends MapRegistry {

    protected static final RenditionProvider DEFAULT_PROVIDER = new DefaultAutomationRenditionProvider();

    @Override
    public void initialize() {
        super.initialize();
        this.<RenditionDefinition> getContributionValues().forEach(desc -> {
            RenditionProvider p = desc.initProvider();
            if (p == null) {
                desc.setProvider(DEFAULT_PROVIDER);
            }
        });
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

    protected boolean canUseRenditionDefinition(RenditionDefinition renditionDefinition, DocumentModel doc) {
        ActionManager actionService = Framework.getService(ActionManager.class);
        return actionService.checkFilters(renditionDefinition.getFilterIds(), createActionContext(doc));
    }

    public RenditionDefinition getRenditionDefinition(String name) {
        if (name == null) {
            return null;
        }
        return this.<RenditionDefinition> getContribution(name)
                   // could be the CMIS name
                   .or(() -> this.<RenditionDefinition> getContributionValues()
                                 .stream()
                                 .filter(desc -> name.equals(desc.getCmisName()))
                                 .findFirst())
                   .orElse(null);
    }

    public List<RenditionDefinition> getRenditionDefinitions(DocumentModel doc) {
        return this.<RenditionDefinition> getContributionValues()
                   .stream()
                   .filter(desc -> canUseRenditionDefinition(desc, doc) && desc.getProvider().isAvailable(doc, desc))
                   .collect(Collectors.toList());
    }

}
