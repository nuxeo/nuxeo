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

import static org.apache.commons.logging.LogFactory.getLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.el.ExpressionContext;
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
        return descriptors.get(name);
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
        ActionContext actionContext = new ELActionContext(new ExpressionContext(), new ExpressionFactoryImpl());
        actionContext.setCurrentDocument(doc);
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
                RenditionProvider provider = definition.getProviderClass().newInstance();
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
        return contrib.clone();
    }

    @Override
    public void merge(RenditionDefinition source, RenditionDefinition dest) {
        if (source.isEnabledSet() && source.isEnabled() != dest.isEnabled()) {
            dest.setEnabled(source.isEnabled());
        }

        String label = source.getLabel();
        if (label != null) {
            dest.setLabel(label);
        }

        String icon = source.getIcon();
        if (icon != null) {
            dest.setIcon(icon);
        }

        String kind = source.getKind();
        if (kind != null) {
            dest.setKind(kind);
        }

        String operationChain = source.getOperationChain();
        if (operationChain != null) {
            dest.setOperationChain(operationChain);
        }

        if (source.isEmptyBlobAllowedSet() && source.isEmptyBlobAllowed() != dest.isEmptyBlobAllowed()) {
            dest.setAllowEmptyBlob(source.isEmptyBlobAllowed());
        }

        if (source.isVisibleSet() && source.isVisible() != dest.isVisible()) {
            dest.setVisible(source.isVisible());
        }

        Class<? extends RenditionProvider> providerClass = source.getProviderClass();
        if (providerClass != null) {
            dest.setProviderClass(providerClass);
        }

        String contentType = source.getContentType();
        if (contentType != null) {
            dest.setContentType(contentType);
        }

        List<String> newFilterIds = new ArrayList<>();
        newFilterIds.addAll(dest.getFilterIds());
        newFilterIds.addAll(source.getFilterIds());
        dest.setFilterIds(newFilterIds);
    }
}
