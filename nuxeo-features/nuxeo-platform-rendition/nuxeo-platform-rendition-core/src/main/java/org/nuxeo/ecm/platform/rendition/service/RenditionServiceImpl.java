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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import static org.nuxeo.ecm.platform.rendition.Constants.FILES_FILES_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.FILES_SCHEMA;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

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
    public List<RenditionDefinition> getAvailableRenditionDefinitions() {
        return new ArrayList<RenditionDefinition>(renditionDefinitions.values());
    }

    @Override
    public DocumentRef render(DocumentModel source,
            String renditionDefinitionName) throws RenditionException {
        RenditionDefinition renditionDefinition = renditionDefinitions.get(renditionDefinitionName);
        if (renditionDefinition == null) {
            String message = "The rendition definition '%s' is not registered";
            throw new RenditionException(String.format(message,
                    renditionDefinitionName), "label.rendition.not.defined");
        }
        return render(source, renditionDefinition);
    }

    protected DocumentRef render(DocumentModel sourceDocument,
            RenditionDefinition renditionDefinition) throws RenditionException {
        validateSourceDocument(sourceDocument);
        try {
            BlobHolder bh = sourceDocument.getAdapter(BlobHolder.class);
            Blob renditionBlob = generateRenditionBlob(bh.getBlob(),
                    renditionDefinition);

            CoreSession session = sourceDocument.getCoreSession();
            DocumentRef versionRef = createVersionIfNeeded(sourceDocument,
                    session);

            DocumentModel version = session.getDocument(versionRef);
            RenditionCreator rc = new RenditionCreator(session, sourceDocument,
                    version, renditionBlob);
            rc.runUnrestricted();

            return rc.getRenditionDocumentRef();
        } catch (ClientException e) {
            throw new RenditionException("Exception while rendering: "
                    + sourceDocument, e);
        }
    }

    /**
     * Checks if the given {@code DocumentModel} can be rendered, throws a
     * {@code RenditionException} if not.
     */
    protected void validateSourceDocument(DocumentModel source)
            throws RenditionException {
        if (source.isProxy()) {
            throw new RenditionException("Cannot render a proxy document");
        }

        BlobHolder bh = source.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new RenditionException("No main file attached",
                    "label.cannot.render.without.main.blob");
        }

        Blob mainBlob;
        try {
            mainBlob = bh.getBlob();
        } catch (ClientException e) {
            throw new RenditionException("Error while retrieving Main Blob", e);
        }

        if (mainBlob == null) {
            throw new RenditionException("No main file attached",
                    "label.cannot.render.without.main.blob");
        }
    }

    protected Blob generateRenditionBlob(Blob sourceBlob,
            RenditionDefinition renditionDefinition) throws RenditionException {
        AutomationService as = getAutomationService();
        OperationContext oc = new OperationContext();
        oc.push(Constants.O_BLOB, sourceBlob);

        try {
            return (Blob) as.run(oc, renditionDefinition.getOperationChain());
        } catch (Exception e) {
            throw new RenditionException(
                    "Exception while running the operation chain: "
                            + renditionDefinition.getOperationChain(), e);
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

    public static class RenditionCreator extends UnrestrictedSessionRunner {

        protected DocumentRef renditionRef;

        protected String liveDocumentId;

        protected DocumentRef versionDocumentRef;

        protected Blob renditionBlob;

        public RenditionCreator(CoreSession session,
                DocumentModel liveDocument, DocumentModel versionDocument,
                Blob renditionBlob) {
            super(session);
            this.liveDocumentId = liveDocument.getId();
            this.versionDocumentRef = versionDocument.getRef();
            this.renditionBlob = renditionBlob;
        }

        public DocumentRef getRenditionDocumentRef() {
            return renditionRef;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel versionDocument = session.getDocument(versionDocumentRef);
            DocumentModel rendition = createRenditionDocument(versionDocument);
            removeBlobs(rendition);
            updateMainBlob(rendition);

            rendition = session.createDocument(rendition);
            renditionRef = rendition.getRef();

            setCorrectVersion(rendition, versionDocument);

            rendition = session.saveDocument(rendition);

            giveReadRightToUser(rendition);
            session.save();
        }

        protected DocumentModel createRenditionDocument(
                DocumentModel versionDocument) throws ClientException {
            DocumentModel rendition = session.createDocumentModel(null,
                    versionDocument.getName(), versionDocument.getType());
            rendition.copyContent(versionDocument);

            rendition.addFacet(RENDITION_FACET);
            rendition.setPropertyValue(RENDITION_SOURCE_ID_PROPERTY,
                    versionDocument.getId());
            rendition.setPropertyValue(
                    RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY, liveDocumentId);
            return rendition;
        }

        protected void removeBlobs(DocumentModel rendition)
                throws ClientException {
            if (rendition.hasSchema(FILES_SCHEMA)) {
                rendition.setPropertyValue(FILES_FILES_PROPERTY,
                        new ArrayList<Map<String, Serializable>>());
            }
        }

        protected void updateMainBlob(DocumentModel rendition)
                throws ClientException {
            BlobHolder bh = rendition.getAdapter(BlobHolder.class);
            bh.setBlob(renditionBlob);
        }

        protected void giveReadRightToUser(DocumentModel rendition)
                throws ClientException {
            ACP acp = new ACPImpl();
            ACL acl = new ACLImpl();
            acp.addACL(acl);
            ACE ace = new ACE(getOriginatingUsername(), SecurityConstants.READ,
                    true);
            acl.add(ace);
            rendition.setACP(acp, true);
        }

        protected void setCorrectVersion(DocumentModel rendition,
                DocumentModel versionDocument) throws ClientException {
            Long minorVersion = (Long) versionDocument.getPropertyValue("uid:minor_version") - 1L;
            rendition.setPropertyValue("uid:minor_version", minorVersion);
            rendition.setPropertyValue("uid:major_version",
                    versionDocument.getPropertyValue("uid:major_version"));
        }

    }

}
