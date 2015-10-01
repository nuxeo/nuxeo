/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.extension.DefaultAutomationRenditionProvider;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.impl.LiveRendition;
import org.nuxeo.ecm.platform.rendition.impl.StoredRendition;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@link RenditionService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public class RenditionServiceImpl extends DefaultComponent implements RenditionService {

    public static final String RENDITION_DEFINITIONS_EP = "renditionDefinitions";

    public static final String RENDITON_DEFINION_PROVIDERS_EP = "renditionDefinitionProviders";

    private static final Log log = LogFactory.getLog(RenditionServiceImpl.class);

    /**
     * @deprecated since 7.2. Not used.
     */
    @Deprecated
    protected AutomationService automationService;

    /**
     * @deprecated since 7.3.
     */
    @Deprecated
    protected Map<String, RenditionDefinition> renditionDefinitions;

    /**
     * @since 7.3. RenditionDefinitions are store in {@link #renditionDefinitionRegistry}.
     */
    protected RenditionDefinitionRegistry renditionDefinitionRegistry;

    protected RenditionDefinitionProviderRegistry renditionDefinitionProviderRegistry;

    @Override
    public void activate(ComponentContext context) {
        renditionDefinitions = new HashMap<>();
        renditionDefinitionRegistry = new RenditionDefinitionRegistry();
        renditionDefinitionProviderRegistry = new RenditionDefinitionProviderRegistry();
        super.activate(context);
    }

    @Override
    public void deactivate(ComponentContext context) {
        renditionDefinitions = null;
        renditionDefinitionRegistry = null;
        renditionDefinitionProviderRegistry = null;
        super.deactivate(context);
    }

    public RenditionDefinition getRenditionDefinition(String name) {
        return renditionDefinitionRegistry.getRenditionDefinition(name);
    }

    @Override
    @Deprecated
    public List<RenditionDefinition> getDeclaredRenditionDefinitions() {
        return new ArrayList<>(renditionDefinitionRegistry.descriptors.values());
    }

    @Override
    @Deprecated
    public List<RenditionDefinition> getDeclaredRenditionDefinitionsForProviderType(String providerType) {
        List<RenditionDefinition> defs = new ArrayList<>();
        for (RenditionDefinition def : getDeclaredRenditionDefinitions()) {
            if (def.getProviderType().equals(providerType)) {
                defs.add(def);
            }
        }
        return defs;
    }

    @Override
    public List<RenditionDefinition> getAvailableRenditionDefinitions(DocumentModel doc) {

        List<RenditionDefinition> defs = new ArrayList<>();
        defs.addAll(renditionDefinitionRegistry.getRenditionDefinitions(doc));
        defs.addAll(renditionDefinitionProviderRegistry.getRenditionDefinitions(doc));

        // XXX what about "lost renditions" ?
        return defs;
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
    public DocumentRef storeRendition(DocumentModel source, String renditionDefinitionName) {

        Rendition rendition = getRendition(source, renditionDefinitionName, true);

        return (rendition == null) ? null : rendition.getHostDocument().getRef();
    }

    protected DocumentModel storeRendition(DocumentModel sourceDocument, List<Blob> renderedBlobs, String name) {
        Blob renderedBlob = renderedBlobs.get(0);
        if (!LazyRendition.isBlobComputationCompleted(renderedBlob)) {
            return null;
        }
        CoreSession session = sourceDocument.getCoreSession();
        DocumentModel version = null;
        boolean isVersionable = sourceDocument.isVersionable();
        if (isVersionable) {
            DocumentRef versionRef = createVersionIfNeeded(sourceDocument, session);
            version = session.getDocument(versionRef);
        }
        RenditionCreator rc = new RenditionCreator(session, sourceDocument, version, renderedBlob, name);
        rc.runUnrestricted();

        DocumentModel detachedRendition = rc.getDetachedRendition();

        detachedRendition.attach(sourceDocument.getSessionId());
        return detachedRendition;
    }

    protected DocumentRef createVersionIfNeeded(DocumentModel source, CoreSession session) {
        DocumentRef versionRef = null;
        if (source.isVersionable()) {
            if (source.isVersion()) {
                versionRef = source.getRef();
            } else if (source.isCheckedOut()) {
                versionRef = session.checkIn(source.getRef(), VersioningOption.MINOR, null);
                source.refresh(DocumentModel.REFRESH_STATE, null);
            } else {
                versionRef = session.getLastDocumentVersionRef(source.getRef());
            }
        }
        return versionRef;
    }

    /**
     * @deprecated since 7.2. Not used.
     */
    @Deprecated
    protected AutomationService getAutomationService() {
        return Framework.getService(AutomationService.class);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (RENDITION_DEFINITIONS_EP.equals(extensionPoint)) {
            RenditionDefinition renditionDefinition = (RenditionDefinition) contribution;
            renditionDefinitionRegistry.addContribution(renditionDefinition);
        } else if (RENDITON_DEFINION_PROVIDERS_EP.equals(extensionPoint)) {
            renditionDefinitionProviderRegistry.addContribution((RenditionDefinitionProviderDescriptor) contribution);
        }
    }

    /**
     * @deprecated since 7.3. RenditionDefinitions are store in {@link #renditionDefinitionRegistry}.
     */
    @Deprecated
    protected void registerRendition(RenditionDefinition renditionDefinition) {
        String name = renditionDefinition.getName();
        if (name == null) {
            log.error("Cannot register rendition without a name");
            return;
        }
        boolean enabled = renditionDefinition.isEnabled();
        if (renditionDefinitions.containsKey(name)) {
            log.info("Overriding rendition with name: " + name);
            if (enabled) {
                renditionDefinition = mergeRenditions(renditionDefinitions.get(name), renditionDefinition);
            } else {
                log.info("Disabled rendition with name " + name);
                renditionDefinitions.remove(name);
            }
        }
        if (enabled) {
            log.info("Registering rendition with name: " + name);
            renditionDefinitions.put(name, renditionDefinition);
        }

        // setup the Provider
        setupProvider(renditionDefinition);
    }

    /**
     * @deprecated since 7.3. RenditionDefinitions are store in {@link #renditionDefinitionRegistry}.
     */
    @Deprecated
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

    protected RenditionDefinition mergeRenditions(RenditionDefinition oldRenditionDefinition,
            RenditionDefinition newRenditionDefinition) {
        String label = newRenditionDefinition.getLabel();
        if (label != null) {
            oldRenditionDefinition.label = label;
        }

        String operationChain = newRenditionDefinition.getOperationChain();
        if (operationChain != null) {
            oldRenditionDefinition.operationChain = operationChain;
        }

        return oldRenditionDefinition;
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (RENDITION_DEFINITIONS_EP.equals(extensionPoint)) {
            renditionDefinitionRegistry.removeContribution((RenditionDefinition) contribution);
        } else if (RENDITON_DEFINION_PROVIDERS_EP.equals(extensionPoint)) {
            renditionDefinitionProviderRegistry.removeContribution((RenditionDefinitionProviderDescriptor) contribution);
        }
    }

    /**
     * @deprecated since 7.3. RenditionDefinitions are store in {@link #renditionDefinitionRegistry}.
     */
    @Deprecated
    protected void unregisterRendition(RenditionDefinition renditionDefinition) {
        String name = renditionDefinition.getName();
        renditionDefinitions.remove(name);
        log.info("Unregistering rendition with name: " + name);
    }

    @Override
    public Rendition getRendition(DocumentModel doc, String renditionName) {
        return getRendition(doc, renditionName, false);
    }

    @Override
    public Rendition getRendition(DocumentModel doc, String renditionName, boolean store) {

        RenditionDefinition renditionDefinition = renditionDefinitionRegistry.getRenditionDefinition(renditionName);
        if (renditionDefinition == null) {
            renditionDefinition = renditionDefinitionProviderRegistry.getRenditionDefinition(renditionName, doc);
            if (renditionDefinition == null) {
                String message = "The rendition definition '%s' is not registered";
                throw new NuxeoException(String.format(message, renditionName));
            }
        }

        if (!renditionDefinition.getProvider().isAvailable(doc, renditionDefinition)) {
            throw new NuxeoException("Rendition " + renditionName + " not available for this doc " + doc.getId());
        }

        DocumentModel stored = null;
        boolean isVersionable = doc.isVersionable();
        if (!isVersionable || !doc.isCheckedOut()) {
            // stored renditions are only done against a non-versionable doc
            // or a versionable doc that is not checkedout
            RenditionFinder finder = new RenditionFinder(doc, renditionName);
            if (isVersionable) {
                finder.runUnrestricted();
            } else {
                finder.run();
            }
            // retrieve the Detached stored rendition doc
            stored = finder.getStoredRendition();
            // re-attach the detached doc
            if (stored != null) {
                stored.attach(doc.getCoreSession().getSessionId());
            }
        }

        if (stored != null) {
            return new StoredRendition(stored, renditionDefinition);
        }

        LiveRendition rendition = new LiveRendition(doc, renditionDefinition);

        if (store) {
            DocumentModel storedRenditionDoc = storeRendition(doc, rendition.getBlobs(), renditionDefinition.getName());
            if (storedRenditionDoc != null) {
                return new StoredRendition(storedRenditionDoc, renditionDefinition);
            } else {
                return rendition;
            }
        } else {
            return rendition;
        }
    }

    @Override
    public List<Rendition> getAvailableRenditions(DocumentModel doc) {
        return getAvailableRenditions(doc, false);
    }

    @Override
    public List<Rendition> getAvailableRenditions(DocumentModel doc, boolean onlyVisible) {
        List<Rendition> renditions = new ArrayList<>();

        if (doc.isProxy()) {
            return renditions;
        }

        List<RenditionDefinition> defs = getAvailableRenditionDefinitions(doc);
        if (defs != null) {
            for (RenditionDefinition def : defs) {
                if (!onlyVisible || onlyVisible && def.isVisible()) {
                    Rendition rendition = getRendition(doc, def.getName());
                    if (rendition != null) {
                        renditions.add(rendition);
                    }
                }
            }
        }

        return renditions;
    }
}
