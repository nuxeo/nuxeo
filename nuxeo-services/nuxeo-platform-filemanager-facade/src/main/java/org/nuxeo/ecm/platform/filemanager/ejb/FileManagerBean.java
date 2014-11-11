/* (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: JNDILocations.java 2992 2006-09-18 09:02:50Z janguenot $
 */

package org.nuxeo.ecm.platform.filemanager.ejb;

import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.ejb.interfaces.local.FileManagerLocal;
import org.nuxeo.runtime.api.Framework;

/**
 * File Manager bean.
 * <p>
 * EJB Facade on the file manager service.
 *
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas
 *         Kalogeropoulos</a>
 */
@Stateless
@Local(FileManagerLocal.class)
@Remote(FileManager.class)
public class FileManagerBean implements FileManagerLocal {

    private static final Log log = LogFactory.getLog(FileManagerBean.class);

    private FileManager service;

    private CoreSession userSession;

    private String userSessionId;

    private FileManager getFileManagerService() throws ClientException {
        if (service == null) {
            service = Framework.getLocalService(FileManager.class);
        }
        if (service == null) {
            log.error("Unable to get local FileManager runtime service");
            throw new ClientException(
                    "Unable to get local FileManager runtime service");
        }
        return service;
    }

    private CoreSession validateSession(CoreSession dm) throws Exception {
        String sid = dm.getSessionId();

        if (CoreInstance.getInstance().isSessionStarted(sid)) {
            // session exists locally : use it :)
            return dm;
        }

        if (sid.equals(userSessionId) && userSession != null) {
            return userSession;
        }

        // if not then try to reconnect
        String repositoryName = dm.getRepositoryName();
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("principal", (Serializable) dm.getPrincipal());
        RepositoryManager repositoryMgr = Framework.getService(RepositoryManager.class);
        Repository repository = repositoryMgr.getRepository(repositoryName);

        userSession = repository.open(context);

        if (userSession == null) {
            throw new ClientException("Unable to connect to Core repository");
        }
        userSessionId = sid;

        return userSession;
    }

    public DocumentModel createDocumentFromBlob(CoreSession documentManager,
            Blob input, String path, boolean overwrite, String fullName)
            throws ClientException {
        try {
            return getFileManagerService().createDocumentFromBlob(
                    validateSession(documentManager), input, path, overwrite,
                    fullName);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public DocumentModel updateDocumentFromBlob(CoreSession documentManager,
            Blob input, String path, String fullName) throws ClientException {
        try {
            return getFileManagerService().updateDocumentFromBlob(
                    validateSession(documentManager), input, path, fullName);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public DocumentModel createFolder(CoreSession documentManager,
            String fullname, String path) throws ClientException {
        try {
            return getFileManagerService().createFolder(
                    validateSession(documentManager), fullname, path);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public String computeDigest(Blob blob) throws ClientException,
            NoSuchAlgorithmException, IOException {
        return getFileManagerService().computeDigest(blob);
    }

    public List<String> getFields() throws ClientException {
        return getFileManagerService().getFields();
    }

    public boolean isUnicityEnabled() throws ClientException {
        return getFileManagerService().isUnicityEnabled();
    }

    public List<DocumentLocation> findExistingDocumentWithFile(
            CoreSession documentManager, String path, String digest,
            Principal principal) throws ClientException {
        return getFileManagerService().findExistingDocumentWithFile(
                documentManager, path, digest, principal);
    }

    public DocumentModelList getCreationContainers(Principal principal,
            String docType) throws Exception {
        return getFileManagerService().getCreationContainers(principal, docType);
    }

    public DocumentModelList getCreationContainers(CoreSession documentManager,
            String docType) throws Exception {
        return getFileManagerService().getCreationContainers(documentManager,
                docType);
    }

    public String getDigestAlgorithm() {
        try {
            return getFileManagerService().getDigestAlgorithm();
        } catch (ClientException e) {
            return null;
        }
    }

    public boolean isDigestComputingEnabled() {
        try {
            return getFileManagerService().isDigestComputingEnabled();
        } catch (ClientException e) {
            return false;
        }
    }

}
