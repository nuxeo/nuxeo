/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.core;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.AdministratorGroupsProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of a {@link org.nuxeo.ecm.directory.Directory Directory} on top of a core repository.
 *
 * @since 8.2
 */
public class CoreDirectory extends AbstractDirectory {

    public static final String DEFAULT_DIRECTORIES_PATH = "/directories"; // NOSONAR

    public static final String DEFAULT_DIRECTORIES_TYPE = "HiddenFolder";

    public static final String DEFAULT_DIRECTORY_TYPE = "Folder";

    protected String repositoryName;

    /** The path of the folder for this directory. */
    protected String directoryPath;

    /** The id of the folder for this directory. */
    protected String directoryFolderId;

    public CoreDirectory(CoreDirectoryDescriptor descriptor) {
        super(descriptor, null);
    }

    @Override
    public CoreDirectoryDescriptor getDescriptor() {
        return (CoreDirectoryDescriptor) descriptor;
    }

    /** Gets the core path where entries for this directory live. */
    public String getDirectoryPath() {
        return directoryPath;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Initialize only basic stuff. The rest of the initialization is called by the CoreDirectoryService
     * RepositoryInitializationHandler when the session is ready.
     */
    @Override
    public void initialize() {
        super.initialize();
        repositoryName = getDescriptor().repositoryName;
        if (StringUtils.isBlank(repositoryName)) {
            repositoryName = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
        }
    }

    /** Called by CoreDirectoryInitializationHandler to finish initializing the directory given a CoreSession. */
    public void initializeCoreSession(CoreSession coreSession) {
        boolean loadData = false;
        boolean save = false;
        CoreDirectoryDescriptor descriptor = getDescriptor();

        String directoriesPath = getDirectoriesPath();
        DocumentModel directories;
        if (!coreSession.exists(new PathRef(directoriesPath))) {
            // create the root of all directories
            // TODO this is not cluster-safe
            int pos = directoriesPath.lastIndexOf('/');
            String directoriesParentPath;
            if (pos == 0) {
                directoriesParentPath = "/";
            } else {
                directoriesParentPath = directoriesPath.substring(0, pos);
            }
            String directoriesName = directoriesPath.substring(pos + 1);
            directories = coreSession.createDocumentModel(directoriesParentPath, directoriesName,
                    descriptor.directoriesType);
            directories.setPropertyValue("dc:title", directoriesName);
            directories = coreSession.createDocument(directories);
            // hide this
            restrictToAdministrators(directories);
            coreSession.saveDocument(directories);
            save = true;
        }

        String tableName = descriptor.tableName == null ? descriptor.name : descriptor.tableName;
        directoryPath = directoriesPath + '/' + tableName;
        DocumentModel directory;
        if (!coreSession.exists(new PathRef(directoryPath))) {
            // create the directory
            // TODO this is not cluster-safe
            directory = coreSession.createDocumentModel(directoriesPath, tableName, descriptor.directoryType);
            directory.setPropertyValue("dc:title", descriptor.name);
            directory = coreSession.createDocument(directory);
            save = true;
            loadData = true;
        } else {
            directory = coreSession.getDocument(new PathRef(directoryPath));
        }
        directoryFolderId = directory.getId();
        if (loadData) {
            loadData();
            // save is already true
        }
        if (save) {
            coreSession.save();
        }
    }

    protected String getDirectoriesPath() {
        String directoriesPath = getDescriptor().directoriesPath;
        if (StringUtils.isBlank(directoriesPath)) {
            directoriesPath = DEFAULT_DIRECTORIES_PATH;
        }
        if (!directoriesPath.startsWith("/")) {
            directoriesPath = "/" + directoriesPath; // NOSONAR
        }
        while (directoriesPath.length() > 1 && directoriesPath.endsWith("/")) {
            directoriesPath = directoriesPath.substring(0, directoriesPath.length() - 1);
        }
        return directoriesPath;
    }

    @Override
    public Session getSession() {
        CoreDirectorySession session = new CoreDirectorySession(this);
        addSession(session);
        return session;
    }

    /** Sets an ACL on the document blocking everyone except administrators. */
    @SuppressWarnings("deprecation")
    protected void restrictToAdministrators(DocumentModel doc) {
        List<String> adminGroups;
        AdministratorGroupsProvider adminGroupsProvider = Framework.getService(AdministratorGroupsProvider.class);
        if (adminGroupsProvider == null) {
            adminGroups = Collections.singletonList(SecurityConstants.ADMINISTRATORS);
        } else {
            adminGroups = adminGroupsProvider.getAdministratorsGroups();
        }
        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        for (String group : adminGroups) {
            acl.add(new ACE(group, SecurityConstants.EVERYTHING, true));
        }
        acl.add(ACE.BLOCK);
        doc.setACP(acp, true);
    }

}
