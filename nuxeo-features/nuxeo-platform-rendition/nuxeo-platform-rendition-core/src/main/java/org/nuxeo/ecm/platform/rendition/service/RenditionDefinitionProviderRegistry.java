/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.rendition.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.el.ExpressionContext;
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
        ActionContext actionContext = new ELActionContext(new ExpressionContext(), new ExpressionFactoryImpl());
        actionContext.setCurrentDocument(doc);
        actionContext.setDocumentManager(doc.getCoreSession());
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
