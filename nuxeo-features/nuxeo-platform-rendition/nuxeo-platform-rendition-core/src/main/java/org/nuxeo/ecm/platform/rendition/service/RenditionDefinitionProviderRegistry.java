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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@link RenditionDefinitionProviderDescriptor} objects.
 *
 * @since 7.2
 */
public class RenditionDefinitionProviderRegistry extends
        ContributionFragmentRegistry<RenditionDefinitionProviderDescriptor> {

    protected Map<String, RenditionDefinitionProviderDescriptor> descriptors = new HashMap<>();

    public List<RenditionDefinition> getRenditionDefinitions(DocumentModel doc) {
        List<RenditionDefinition> renditionDefinitions = new ArrayList<>();

        for (RenditionDefinitionProviderDescriptor descriptor : descriptors.values()) {
            if (canUseRenditionDefinitionProvider(descriptor, doc)) {
                RenditionDefinitionProvider provider = descriptor.getProvider();
                renditionDefinitions.addAll(provider.getRenditionDefinitions(doc));
            }
        }

        return renditionDefinitions;
    }

    public RenditionDefinition getRenditionDefinition(String name, DocumentModel doc) {
        List<RenditionDefinition> renditionDefinitions = getRenditionDefinitions(doc);
        for (RenditionDefinition renditionDefinition : renditionDefinitions) {
            if (renditionDefinition.getName().equals(name)) {
                return renditionDefinition;
            }
        }
        return null;
    }

    protected boolean canUseRenditionDefinitionProvider(
            RenditionDefinitionProviderDescriptor renditionDefinitionProviderDescriptor, DocumentModel doc) {
        ActionManager actionService = Framework.getService(ActionManager.class);
        return actionService.checkFilters(renditionDefinitionProviderDescriptor.getFilterIds(),
                createActionContext(doc));
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
    public String getContributionId(RenditionDefinitionProviderDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, RenditionDefinitionProviderDescriptor contrib,
            RenditionDefinitionProviderDescriptor newOrigContrib) {
        if (contrib.isEnabled()) {
            descriptors.put(id, contrib);
        } else {
            descriptors.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id, RenditionDefinitionProviderDescriptor contrib) {
        descriptors.remove(id);
    }

    @Override
    public RenditionDefinitionProviderDescriptor clone(RenditionDefinitionProviderDescriptor contrib) {
        return contrib.clone();
    }

    @Override
    public void merge(RenditionDefinitionProviderDescriptor source, RenditionDefinitionProviderDescriptor dest) {
        if (source.isEnabledSet() && source.isEnabled() != dest.isEnabled()) {
            dest.setEnabled(source.isEnabled());
        }

        Class<? extends RenditionDefinitionProvider> providerClass = source.getProviderClass();
        if (providerClass != null) {
            dest.setProviderClass(providerClass);
        }

        List<String> newFilterIds = new ArrayList<>();
        newFilterIds.addAll(dest.getFilterIds());
        newFilterIds.addAll(source.getFilterIds());
        dest.setFilterIds(newFilterIds);
    }
}
