/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mhilaire
 */
package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.LoginContext;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryFactory;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Names;

/**
 *
 *
 * @since 5.9.6
 */
@Features({ ClientLoginFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.directory.types.contrib",
        "org.nuxeo.ecm.directory.sql" })
public class SQLDirectoryFeature extends SimpleFeature {
    public static final String USER_DIRECTORY_NAME = "userDirectory";

    public static final String GROUP_DIRECTORY_NAME = "groupDirectory";

    @Inject
    DirectoryService directoryService;

    @Override
    public void configure(final FeaturesRunner runner, Binder binder) {
        bindDirectory(binder, USER_DIRECTORY_NAME);
        bindDirectory(binder, GROUP_DIRECTORY_NAME);
    }

    protected void bindDirectory(Binder binder, final String name) {
        binder.bind(Directory.class).annotatedWith(Names.named(name)).toProvider(
                new Provider<Directory>() {

                    @Override
                    public Directory get() {
                        return Framework.getService(DirectoryService.class).getDirectory(
                                name);
                    }

                });
    }

    protected final Map<Directory, Set<DocumentModel>> savedContext = new HashMap<>();

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        LoginContext lc = Framework.login();
        try {
            MemoryDirectoryFactory memoryDirectoryFactory = new MemoryDirectoryFactory();
            Framework.getService(
                    DirectoryService.class).registerDirectory("memdirs",
                    memoryDirectoryFactory);
            List<Directory> directories = Framework.getService(
                    DirectoryService.class).getDirectories();
            for (Directory dir : directories) {

                MemoryDirectory memdir1 = new MemoryDirectory(dir.getName()
                        + "-orig", dir.getSchema(), dir.getIdField(),
                        dir.getPasswordField());
                memoryDirectoryFactory.registerDirectory(memdir1);

                Session session = dir.getSession();
                try {
                    Map<String, Serializable> filter = Collections.emptyMap();
            
                    DocumentModelList entries = session. query(filter);
                    Session memdir1Session = memdir1.getSession();
                    try {

                        for (DocumentModel docModel : entries) {
                            memdir1Session.createEntry(session.getEntry(docModel.getId(),true));
                        }
                    } finally {
                        memdir1Session.close();
                    }

                } finally {
                    session.close();
                }
            }
        } finally {
            lc.logout();
        }
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        LoginContext lc = Framework.login();
        try {
            List<Directory> directories = Framework.getService(
                    DirectoryService.class).getDirectories();
            for (Directory dir : directories) {

                if (!dir.getName().endsWith("-orig")) {
                    Session session = dir.getSession();
                    try {
                        Map<String, Serializable> filter = Collections.emptyMap();

                        DocumentModelList entries = session.query(filter);

                        Session memdir1Session = Framework.getService(
                                DirectoryService.class).getDirectory(
                                dir.getName() + "-orig").getSession();
                        DocumentModelList origEntries = memdir1Session.query(filter);
                        try {

                            for (DocumentModel docModel : entries) {
                                session.deleteEntry(docModel.getId());
                            }
                            for (DocumentModel origEntry : origEntries) {
                                session.createEntry(origEntry);
                            }
                        } finally {
                            memdir1Session.close();
                        }

                    } finally {
                        session.close();
                    }
                }
            }
         
        } finally {
            lc.logout();
        }

    }
}
