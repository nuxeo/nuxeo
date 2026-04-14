/*
 * (C) Copyright 2017-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */
package org.nuxeo.directory.test;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.DIRECTORY_SERVICE_VALUE;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_MONGODB;
import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.STORAGE_SQL;
import static org.nuxeo.common.test.logging.NuxeoLoggingConstants.MARKER_CONSOLE_OVERRIDE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.directory.mongodb.MongoDBDirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryCoreFeature;
import org.nuxeo.ecm.directory.DirectoryDeleteConstraintException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.multi.MultiDirectory;
import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Cleanup.Granularity;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.DynamicFeaturesLoader;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Binder;
import com.google.inject.name.Names;

/**
 * @since 9.2
 */
@Deploy("org.nuxeo.directory.test")
@Features(DirectoryCoreFeature.class)
public class DirectoryFeature implements RunnerFeature {

    private static final Logger log = LogManager.getLogger(DirectoryFeature.class);

    public static final String USER_DIRECTORY_NAME = "userDirectory";

    public static final String GROUP_DIRECTORY_NAME = "groupDirectory";

    protected Granularity granularity;

    protected Map<String, Map<String, Map<String, Object>>> allDirectoryData;

    public DirectoryFeature(DynamicFeaturesLoader loader) {
        var feature = switch (DIRECTORY_SERVICE_VALUE) {
            case STORAGE_MONGODB -> MongoDBDirectoryFeature.class;
            case STORAGE_SQL -> SQLDirectoryFeature.class;
            default -> throw new UnsupportedOperationException(
                    "Directory type: " + DIRECTORY_SERVICE_VALUE + " is not supported");
        };
        loader.loadFeature(feature);
    }

    @Override
    @SuppressWarnings("removal") // deprecated since 2025.19, get granularity configuration from @Cleanup
    public void start(FeaturesRunner runner) {
        log.info(MARKER_CONSOLE_OVERRIDE, "Deploying Directory using {}",
                () -> StringUtils.capitalize(DIRECTORY_SERVICE_VALUE.toLowerCase()));
        granularity = Optional.ofNullable(runner.getFeature(CoreFeature.class))
                              .map(CoreFeature::getGranularity)
                              .filter(granularity -> granularity != org.nuxeo.ecm.core.test.annotations.Granularity.METHOD)
                              .map(granularity -> Granularity.CLASS)
                              .orElse(Granularity.METHOD);
    }

    @Override
    public void configure(final FeaturesRunner runner, Binder binder) {
        bindDirectory(binder, USER_DIRECTORY_NAME);
        bindDirectory(binder, GROUP_DIRECTORY_NAME);
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        if (granularity != Granularity.METHOD) {
            return;
        }
        // record all directories in their entirety
        allDirectoryData = new HashMap<>();
        DirectoryService directoryService = Framework.getService(DirectoryService.class);

        TransactionHelper.runInTransaction(() -> Framework.doPrivileged(() -> {
            for (Directory dir : directoryService.getDirectories()) {
                // Do not save multi-directories as subdirectories will be saved
                if (dir.isReadOnly() || dir instanceof MultiDirectory) {
                    continue;
                }
                try (Session session = dir.getSession()) {
                    session.setReadAllColumns(true); // needs to fetch the password too
                    List<DocumentModel> entries = session.query(Collections.emptyMap(), Collections.emptySet(),
                            Collections.emptyMap(), true); // fetch references
                    Map<String, Map<String, Object>> data = entries.stream()
                                                                   .collect(Collectors.toMap(DocumentModel::getId,
                                                                           entry -> entry.getProperties(
                                                                                   dir.getSchema())));
                    allDirectoryData.put(dir.getName(), data);
                }
            }
        }));
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        if (granularity != Granularity.METHOD) {
            return;
        }
        if (allDirectoryData == null) {
            // failure (exception or assumption failed) before any method was run
            return;
        }

        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        TransactionHelper.runInTransaction(() -> Framework.doPrivileged(() -> {
            // clear all directories
            boolean isAllClear;
            do {
                isAllClear = true;
                for (Directory dir : directoryService.getDirectories()) {
                    // Do not purge multi-directories as subdirectories will be purged
                    if (dir.isReadOnly() || dir instanceof MultiDirectory) {
                        continue;
                    }
                    try (Session session = dir.getSession()) {
                        List<String> ids = session.getProjection(Collections.emptyMap(), dir.getIdField());
                        for (String id : ids) {
                            try {
                                session.deleteEntry(id);
                            } catch (DirectoryDeleteConstraintException e) {
                                isAllClear = false;
                            }
                        }
                    }
                }
            } while (!isAllClear);
            // re-create all directory entries
            for (Map.Entry<String, Map<String, Map<String, Object>>> each : allDirectoryData.entrySet()) {
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
        }));
        allDirectoryData = null;
    }

    protected void bindDirectory(Binder binder, final String name) {
        binder.bind(Directory.class)
              .annotatedWith(Names.named(name))
              .toProvider(() -> Framework.getService(DirectoryService.class).getDirectory(name));
    }

}
