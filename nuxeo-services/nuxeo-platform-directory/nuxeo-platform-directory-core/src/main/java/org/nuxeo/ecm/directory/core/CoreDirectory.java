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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFieldMapper;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of a {@link Directory} on top of a core repository.
 *
 * @since 8.2
 */
public class CoreDirectory extends AbstractDirectory {

    private static final Log log = LogFactory.getLog(CoreDirectory.class);

    protected final Schema schema;

    public CoreDirectory(CoreDirectoryDescriptor descriptor) {
        super(descriptor);
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        schema = schemaManager.getSchema(descriptor.schemaName);
        fieldMapper = new DirectoryFieldMapper(descriptor.fieldMapping);
        if (schema == null) {
            throw new DirectoryException(
                    String.format("Unknown schema '%s' for directory '%s' ", descriptor.schemaName, getName()));
        }
        start();
    }

    @Override
    public CoreDirectoryDescriptor getDescriptor() {
        return (CoreDirectoryDescriptor) descriptor;
    }

    public void start() {
        CoreDirectoryDescriptor descriptor = getDescriptor();
        UnrestrictedSessionRunner directoryInitializer = new UnrestrictedSessionRunner(descriptor.getRepositoryName()) {

            @Override
            public void run() {
                String createPath = descriptor.getCreatePath();

                DocumentModel rootFolder = null;
                DocumentRef rootRef = new PathRef(createPath);
                if (session.exists(rootRef)) {
                    rootFolder = session.getDocument(rootRef);
                }

                if (rootFolder == null) {

                    String parentFolder = createPath.substring(0, createPath.lastIndexOf("/"));
                    if (createPath.lastIndexOf("/") == 0) {
                        parentFolder = "/";
                    }
                    String createFolder = createPath.substring(createPath.lastIndexOf("/") + 1, createPath.length());

                    log.info(String.format(
                            "Root folder '%s' has not been found for the directory '%s' on the repository '%s', will create it with given ACL",
                            createPath, getName(), descriptor.getRepositoryName()));
                    if (descriptor.canCreateRootFolder()) {
                        try {
                            DocumentModel doc = session.createDocumentModel(parentFolder, createFolder, "Folder");
                            doc.setProperty("dublincore", "title", createFolder);
                            session.createDocument(doc);
                            // Set ACL from descriptor
                            for (int i = 0; i < descriptor.acls.length; i++) {
                                String userOrGroupName = descriptor.acls[i].userOrGroupName;
                                String privilege = descriptor.acls[i].privilege;
                                boolean granted = descriptor.acls[i].granted;
                                setACL(doc, userOrGroupName, privilege, granted);
                            }
                            session.save();

                        } catch (DocumentNotFoundException e) {
                            throw new DirectoryException(String.format(
                                    "The root folder '%s' can not be created under '%s' for the directory '%s' on the repository '%s',"
                                            + " please make sure you have set the right path or that the path exist",
                                    createFolder, parentFolder, getName(), descriptor.getRepositoryName()), e);
                        }
                    }

                } else {
                    log.info(String.format(
                            "Root folder '%s' has been found for the directory '%s' on the repository '%s', ACL will not be set",
                            createPath, getName(), descriptor.getRepositoryName()));
                }

            }
        };
        directoryInitializer.runUnrestricted();
    }

    protected DocumentModel setACL(DocumentModel rootFolder, String userOrGroupName, String privilege,
            boolean granted) {
        ACP acp = rootFolder.getACP();
        ACL localACL = acp.getOrCreateACL();
        localACL.add(new ACE(userOrGroupName, privilege, granted));
        rootFolder.setACP(acp, true);

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Set ACL on root folder '%s' : userOrGroupName = '%s', privilege = '%s' , granted = '%s' ",
                    rootFolder.getPathAsString(), userOrGroupName, privilege, granted));
        }

        return rootFolder.getCoreSession().saveDocument(rootFolder);
    }

    public Field getField(String name) throws DirectoryException {
        Field field = schema.getField(name);
        if (field == null) {
            throw new DirectoryException(
                    String.format("Field '%s' does not exist in the schema '%s'", name, schema.getName()));
        }
        return field;
    }

    @Override
    public Session getSession() throws DirectoryException {
        CoreDirectorySession session = new CoreDirectorySession(this);
        addSession(session);
        return session;
    }

}
