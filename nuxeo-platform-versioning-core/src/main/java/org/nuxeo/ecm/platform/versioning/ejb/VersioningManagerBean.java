/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.ejb;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateless;
import javax.naming.NamingException;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningChangeNotifier;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.versioning.api.DocVersion;
import org.nuxeo.ecm.platform.versioning.api.SnapshotOptions;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningException;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.platform.versioning.service.ServiceHelper;
import org.nuxeo.ecm.platform.versioning.service.VersioningService;
import org.nuxeo.ecm.platform.versioning.wfintf.WFVersioningPolicyProvider;

/**
 * This is a versioning EJB facade.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Stateless
@Local(VersioningManager.class)
@Remote(VersioningManager.class)
public class VersioningManagerBean implements VersioningManager {

    private static final Log log = LogFactory.getLog(VersioningManagerBean.class);

    @Transient
    private transient VersioningService service;

    @PostConstruct
    public void ejbCreate() {
        log.debug("PostConstruct");
        initService();
    }

    @PostActivate
    public void ejbActivate() {
        log.debug("PostActivate");
        initService();
    }

    @PrePassivate
    public void ejbPassivate() {
        log.debug("PrePassivate");
    }

    @Remove
    public void ejbRemove() {
        log.debug("Remove");
    }

    private void initService() {
        if (service == null) {
            service = ServiceHelper.getVersioningService();
        }
    }

    protected Map<String, Object> getDocumentManagerProperties() {
        Map<String, Object> props = new HashMap<String, Object>();
        // :XXX: use constants
        props.put("participant", new UserPrincipal(
                SecurityConstants.ADMINISTRATOR));
        return props;
    }

    public VersionIncEditOptions getVersionIncEditOptions(DocumentModel docModel)
            throws VersioningException, ClientException, DocumentException {
        return service.getVersionIncEditOptions(docModel);
    }

    @Deprecated
    public VersionIncEditOptions getVersionIncOptions(DocumentRef docRef,
            CoreSession documentManager) throws VersioningException, ClientException {

        final String logPrefix = "<getVersionIncOptions> ";

        // check with VersioningService
        // final DocumentModel doc = getDocumentModel(docRef);

        // final DocumentManager documentManager = getDocumentManager();
        final String currentLifeCycleState;
        final String documentType;
        final DocumentModel dm;
        try {
            currentLifeCycleState = documentManager.getCurrentLifeCycleState(docRef);
            dm = documentManager.getDocument(docRef);
            documentType = dm.getType();
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            throw new VersioningException(
                    "Error getting currentLifeCycleState", e);
        }
        log.debug(logPrefix + "currentLifeCycleState: " + currentLifeCycleState);

        VersionIncEditOptions options = null;

        if (currentLifeCycleState != null) {
            log.debug("check versioning policy in component extensions");
            options = service.getVersionIncOptions(currentLifeCycleState,
                    documentType);

            if (options.getVersioningAction() == VersioningActions.ACTION_CASE_DEPENDENT) {
                return options;
            }
        } else {
            log.warn(logPrefix + "document lifecycle not initialized.");
        }

        // check with document Workflow
        log.debug(logPrefix + "check versioning policy in document workflow");
        final VersioningActions wfvaction = WFVersioningPolicyProvider.getVersioningPolicyFor(dm);
        log.debug(logPrefix + "wfvaction = " + wfvaction);
        options = new VersionIncEditOptions();
        if (wfvaction != null) {
            // return null;// wfvaction;
            if (wfvaction == VersioningActions.ACTION_CASE_DEPENDENT) {
                options.addOption(VersioningActions.ACTION_NO_INCREMENT);
                options.addOption(VersioningActions.ACTION_INCREMENT_MINOR);
            } else {
                // because LE needs options we add the option received from WF
                options.addOption(wfvaction);
            }
        } else {
            // XXX wf action is null!!?
            log.error(logPrefix + "wf action is null");
        }

        return options;

        // XXX fallback if the WF does not specifically define it
    }

    private CoreSession getDocumentManager() throws VersioningException {
        try {
            return EjbLocator.getDocumentManager();
            // return __getDocumentManager();
        } catch (NamingException e) {
            throw new VersioningException("Error getting DocumentManager", e);
        }
    }

    private DocumentModel getDocumentModel(DocumentRef docRef)
            throws VersioningException {

        final CoreSession documentManager = getDocumentManager();

        try {
            return documentManager.getDocument(docRef);
        } catch (ClientException e) {
            throw new VersioningException("Error accessing DocumentModel", e);
        }
    }

    public void remove() {
        // TODO Auto-generated method stub
    }

    public DocumentModel incrementMajor(DocumentModel doc)
            throws ClientException {
        return service.incrementMajor(doc);
    }

    public DocumentModel incrementMinor(DocumentModel doc)
            throws ClientException {
        return service.incrementMinor(doc);
    }

    public String getMajorVersionPropertyName(String documentType) {
        return service.getMajorVersionPropertyName(documentType);
    }

    public String getMinorVersionPropertyName(String documentType) {
        return service.getMinorVersionPropertyName(documentType);
    }

    public String getVersionLabel(DocumentModel doc) throws ClientException {
        return service.getVersionLabel(doc);
    }

    public DocVersion getNextVersion(DocumentModel doc) throws ClientException {
        return service.getNextVersion(doc);
    }

    @Deprecated
    public void notifyVersionChange(DocumentModel oldDocument,
            DocumentModel newDocument) {
        VersioningChangeNotifier.notifyVersionChange(oldDocument, newDocument, null);
    }

    public SnapshotOptions getCreateSnapshotOption(DocumentModel document) throws ClientException {
        return service.getCreateSnapshotOption(document);
    }

}
