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
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.extension.DefaultAutomationRenditionProvider;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
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
public class RenditionServiceImpl extends DefaultComponent implements
        RenditionService {

    public static final String RENDITION_DEFINITIONS_EP = "renditionDefinitions";

    private static final Log log = LogFactory.getLog(RenditionServiceImpl.class);

    protected AutomationService automationService;

    protected Map<String, RenditionDefinition> renditionDefinitions;

    @Override
    public void activate(ComponentContext context) throws Exception {
        renditionDefinitions = new HashMap<String, RenditionDefinition>();
        super.activate(context);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        renditionDefinitions = null;
        super.deactivate(context);
    }

    @Override
    public List<RenditionDefinition> getDeclaredRenditionDefinitions() {
        return new ArrayList<RenditionDefinition>(renditionDefinitions.values());
    }

    @Override
    public List<RenditionDefinition> getDeclaredRenditionDefinitionsForProviderType(
            String providerType) {
        List<RenditionDefinition> defs = new ArrayList<RenditionDefinition>();
        for (RenditionDefinition def : getDeclaredRenditionDefinitions()) {
            if (def.getProviderType().equals(providerType)) {
                defs.add(def);
            }
        }
        return defs;
    }

    @Override
    public List<RenditionDefinition> getAvailableRenditionDefinitions(
            DocumentModel doc) {
        List<RenditionDefinition> defs = new ArrayList<RenditionDefinition>();
        for (RenditionDefinition def : renditionDefinitions.values()) {
            if (def.getProvider().isAvailable(doc, def)) {
                defs.add(def);
            }
        }
        // XXX what about "lost renditions" ?
        return defs;
    }

    @Override
    public DocumentRef storeRendition(DocumentModel source,
            String renditionDefinitionName) throws RenditionException {

        Rendition rendition = getRendition(source, renditionDefinitionName,
                true);

        return rendition.getHostDocument().getRef();
    }

    protected DocumentModel storeRendition(DocumentModel sourceDocument,
            List<Blob> renderedBlobs, String name) throws RenditionException {
        try {
            CoreSession session = sourceDocument.getCoreSession();
            DocumentRef versionRef = createVersionIfNeeded(sourceDocument,
                    session);

            DocumentModel version = session.getDocument(versionRef);
            RenditionCreator rc = new RenditionCreator(session, sourceDocument,
                    version, renderedBlobs.get(0), name);
            rc.runUnrestricted();

            DocumentModel detachedRendition = rc.getDetachedDendition();

            detachedRendition.attach(sourceDocument.getSessionId());
            return detachedRendition;
        } catch (Exception e) {
            throw new RenditionException("Unable to store rendition", e);
        }
    }

    protected DocumentRef createVersionIfNeeded(DocumentModel source,
            CoreSession session) throws ClientException {
        DocumentRef versionRef;
        if (source.isVersion()) {
            versionRef = source.getRef();
        } else if (source.isCheckedOut()) {
            versionRef = session.checkIn(source.getRef(),
                    VersioningOption.MINOR, null);
            source.refresh(DocumentModel.REFRESH_STATE, null);
        } else {
            versionRef = session.getLastDocumentVersionRef(source.getRef());
        }
        return versionRef;
    }

    protected AutomationService getAutomationService()
            throws RenditionException {
        if (automationService == null) {
            try {
                automationService = Framework.getService(AutomationService.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to AutomationService. "
                        + e.getMessage();
                throw new RenditionException(errMsg, "", e);
            }
            if (automationService == null) {
                throw new RenditionException(
                        "AutomationService service not bound");
            }
        }
        return automationService;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (RENDITION_DEFINITIONS_EP.equals(extensionPoint)) {
            registerRendition((RenditionDefinition) contribution);
        }
    }

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
                renditionDefinition = mergeRenditions(
                        renditionDefinitions.get(name), renditionDefinition);
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

    protected RenditionDefinition mergeRenditions(
            RenditionDefinition oldRenditionDefinition,
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
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (RENDITION_DEFINITIONS_EP.equals(extensionPoint)) {
            unregisterRendition((RenditionDefinition) contribution);
        }
    }

    protected void unregisterRendition(RenditionDefinition renditionDefinition) {
        String name = renditionDefinition.getName();
        renditionDefinitions.remove(name);
        log.info("Unregistering rendition with name: " + name);
    }

    @Override
    public Rendition getRendition(DocumentModel doc, String renditionName)
            throws RenditionException {
        return getRendition(doc, renditionName, false);
    }

    @Override
    public Rendition getRendition(DocumentModel doc, String renditionName,
            boolean store) throws RenditionException {

        RenditionDefinition renditionDefinition = renditionDefinitions.get(renditionName);
        if (renditionDefinition == null) {
            String message = "The rendition definition '%s' is not registered";
            throw new RenditionException(String.format(message, renditionName),
                    "label.rendition.not.defined");
        }

        if (!renditionDefinition.getProvider().isAvailable(doc, renditionDefinition)) {
            throw new RenditionException("Rendition " + renditionName + " not available for this doc " + doc.getId());
        }
        
        DocumentModel stored = null;
        try {
            if (!doc.isCheckedOut()) {
                // since stored renditions are done against a version
                // checkedout Documents can not have a stored rendition
                RenditionFinder finder = new RenditionFinder(doc, renditionName);
                finder.runUnrestricted();
                // retrieve the Detached stored rendition doc
                stored = finder.getStoredRendition();
                // re-attach the detached doc
                if (stored != null) {
                    stored.attach(doc.getCoreSession().getSessionId());
                }
            }
        } catch (ClientException e) {
            throw new RenditionException(
                    "Error while searching for stored rendition", e);
        }

        if (stored != null) {
            return new StoredRendition(stored, renditionDefinition);
        }

        LiveRendition rendition = new LiveRendition(doc, renditionDefinition);

        if (store) {
            DocumentModel storedRenditionDoc = storeRendition(doc,
                    rendition.getBlobs(), renditionDefinition.getName());
            return new StoredRendition(storedRenditionDoc, renditionDefinition);

        } else {
            return rendition;
        }
    }

    public List<Rendition> getAvailableRenditions(DocumentModel doc)
            throws RenditionException {

        List<Rendition> renditions = new ArrayList<Rendition>();

        if (doc.isProxy()) {
            return renditions;
        }

        List<RenditionDefinition> defs = getAvailableRenditionDefinitions(doc);
        if (defs != null) {
            for (RenditionDefinition def : defs) {
                Rendition rendition = getRendition(doc, def.getName());
                if (rendition != null) {
                    renditions.add(rendition);
                }
            }
        }

        return renditions;
    }
}
