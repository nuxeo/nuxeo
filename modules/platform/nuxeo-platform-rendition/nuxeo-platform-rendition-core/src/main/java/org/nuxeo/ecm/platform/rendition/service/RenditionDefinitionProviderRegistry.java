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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Registry for {@link RenditionDefinitionProviderDescriptor} objects.
 *
 * @since 7.2
 */
public class RenditionDefinitionProviderRegistry extends MapRegistry {

    @Override
    public void initialize() {
        super.initialize();
        this.<RenditionDefinitionProviderDescriptor> getContributionValues()
            .forEach(RenditionDefinitionProviderDescriptor::initProvider);
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

    protected boolean canUseRenditionDefinitionProvider(
            RenditionDefinitionProviderDescriptor renditionDefinitionProviderDescriptor, DocumentModel doc) {
        ActionManager actionService = Framework.getService(ActionManager.class);
        return actionService.checkFilters(renditionDefinitionProviderDescriptor.getFilterIds(),
                createActionContext(doc));
    }

    protected Stream<RenditionDefinition> streamRenditionDefinitions(DocumentModel doc) {
        return this.<RenditionDefinitionProviderDescriptor> getContributions()
                   .values()
                   .stream()
                   .filter(desc -> canUseRenditionDefinitionProvider(desc, doc))
                   .map(RenditionDefinitionProviderDescriptor::getProvider)
                   .filter(Objects::nonNull)
                   .map(provider -> provider.getRenditionDefinitions(doc))
                   .flatMap(List::stream);
    }

    public List<RenditionDefinition> getRenditionDefinitions(DocumentModel doc) {
        return streamRenditionDefinitions(doc).collect(Collectors.toList());
    }

    public RenditionDefinition getRenditionDefinition(String name, DocumentModel doc) {
        if (name == null) {
            return null;
        }
        return streamRenditionDefinitions(doc).filter(rd -> name.equals(rd.getName())).findFirst().orElse(null);
    }

}
