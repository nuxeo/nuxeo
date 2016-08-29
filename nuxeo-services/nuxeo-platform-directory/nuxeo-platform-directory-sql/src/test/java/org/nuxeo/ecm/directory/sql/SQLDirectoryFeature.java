/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mhilaire
 */
package org.nuxeo.ecm.directory.sql;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryDeleteConstraintException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.name.Names;

/**
 * @since 6.0
 */
@Features({ CoreFeature.class, ClientLoginFeature.class })
@Deploy({ "org.nuxeo.ecm.directory.api", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.core.schema", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.directory.sql" })
@LocalDeploy("org.nuxeo.ecm.directory.sql:nxdirectory-ds.xml")
public class SQLDirectoryFeature extends SimpleFeature {
    public static final String USER_DIRECTORY_NAME = "userDirectory";

    public static final String GROUP_DIRECTORY_NAME = "groupDirectory";

    @Override
    public void configure(final FeaturesRunner runner, Binder binder) {
        bindDirectory(binder, USER_DIRECTORY_NAME);
        bindDirectory(binder, GROUP_DIRECTORY_NAME);
    }

    protected void bindDirectory(Binder binder, final String name) {
        binder.bind(Directory.class).annotatedWith(Names.named(name)).toProvider(new Provider<Directory>() {

            @Override
            public Directory get() {
                return Framework.getService(DirectoryService.class).getDirectory(name);
            }

        });
    }

    Granularity granularity;

    protected Map<String, Map<String, Map<String, Object>>> allDirectoryData;

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        granularity = runner.getFeature(CoreFeature.class).getGranularity();
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        if (granularity != Granularity.METHOD) {
            return;
        }
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        // record all directories in their entirety
        allDirectoryData = new HashMap<>();
        for (Directory dir : directoryService.getDirectories()) {
            Map<String, Map<String, Object>> data = new HashMap<>();
            try (Session session = dir.getSession()) {
                List<DocumentModel> entries = session.query(Collections.emptyMap(), Collections.emptySet(),
                        Collections.emptyMap(), true); // fetch references
                for (DocumentModel entry : entries) {
                    DataModel dm = entry.getDataModel(dir.getSchema());
                    data.put(entry.getId(), dm.getMap());
                }
                allDirectoryData.put(dir.getName(), data);
            }
        }
        // commit the transaction so that lazily-created relation tables are committed
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        if (granularity != Granularity.METHOD) {
            return;
        }
        if (allDirectoryData == null) {
            // failure (exception or assumption failed) before any method was run
            return;
        }
        // system user to bypass directory security
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(new SystemPrincipal(null), null, null);
        try {
            DirectoryService directoryService = Framework.getService(DirectoryService.class);
            // clear all directories
            boolean isAllClear = true;
            do {
                isAllClear = true;
                for (Directory dir : directoryService.getDirectories()) {
                    try (Session session = dir.getSession()) {
                        List<String> ids = session.getProjection(Collections.emptyMap(), dir.getIdField());
                        for (String id : ids) {
                            try {
                                session.deleteEntry(id);
                            } catch(DirectoryDeleteConstraintException e) {
                                isAllClear = false;
                            }
                        }
                    }
                }
            } while(!isAllClear);
            // re-create all directory entries
            for (Entry<String, Map<String, Map<String, Object>>> each : allDirectoryData.entrySet()) {
                String directoryName = each.getKey();
                Directory directory = directoryService.getDirectory(directoryName);
                Collection<Map<String, Object>> data = each.getValue().values();
                try (Session session = directory.getSession()) {
                    for (Map<String, Object> map : data) {
                        try {
                            session.createEntry(map);
                        } catch (DirectoryException e) {
                            // happens for filter directories
                            // or when testing config changes
                            if (!e.getMessage().contains("already exists") && !e.getMessage().contains("Missing id")) {
                                throw e;
                            }
                        }
                    }
                }
            }
        } finally {
            loginStack.pop();
        }
        allDirectoryData = null;
    }
}
