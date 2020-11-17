/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment;

import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_ID;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.migration.MigrationDescriptor;
import org.nuxeo.runtime.migration.MigrationServiceImpl;
import org.nuxeo.runtime.model.impl.ComponentManagerImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import com.google.inject.Binder;
import com.google.inject.name.Names;

/**
 * This feature allows to put the {@link org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager} service into the
 * services container. It configures what {@link org.nuxeo.runtime.migration.MigrationService} needs to get a running
 * status, and configures the running step by getting the one having fromState == currentState.
 * <p>
 * This feature doesn't trigger the migration!
 * <p>
 * Due to its design, this feature can't be used as it because there should not be a migration step having the supported
 * implementation. It should be used with {@link RelationCommentFeature} or {@link PropertyCommentFeature}.
 *
 * @since 11.1
 */
@Features(CommentFeature.class)
public class BridgeCommentFeature implements RunnerFeature {

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(CommentManager.class)
              .annotatedWith(Names.named("first"))
              .toProvider(() -> ((BridgeCommentManager) Framework.getService(CommentManager.class)).getFirst());
        binder.bind(CommentManager.class)
              .annotatedWith(Names.named("second"))
              .toProvider(() -> ((BridgeCommentManager) Framework.getService(CommentManager.class)).getSecond());
    }

    @Override
    public void beforeRun(FeaturesRunner runner) {
        var componentManager = (ComponentManagerImpl) Framework.getRuntime().getComponentManager();
        MigrationDescriptor descriptor = componentManager.getDescriptors()
                                                         .getDescriptor("org.nuxeo.runtime.migration.MigrationService",
                                                                 MigrationServiceImpl.XP_CONFIG, MIGRATION_ID);
        String defaultState = descriptor.getDefaultState();
        String migrationStep = descriptor.getSteps()
                                         .values()
                                         .stream()
                                         .filter(s -> defaultState.equals(s.getFromState()))
                                         .findFirst()
                                         .map(MigrationDescriptor.MigrationStepDescriptor::getId)
                                         .orElseThrow(() -> new AssertionError(
                                                 "Unable to find the step to set from state: " + defaultState));

        // set bridge in service container by setting a step in migration service (without running it)
        KeyValueStore kv = Framework.getService(KeyValueService.class)
                                    .getKeyValueStore(MigrationServiceImpl.KEYVALUE_STORE_NAME);
        kv.put(MIGRATION_ID + MigrationServiceImpl.STEP, migrationStep);
        kv.put(MIGRATION_ID + MigrationServiceImpl.START_TIME, String.valueOf(System.currentTimeMillis()));
        kv.put(MIGRATION_ID + MigrationServiceImpl.PING_TIME, String.valueOf(System.currentTimeMillis()));
        kv.put(MIGRATION_ID + MigrationServiceImpl.PROGRESS_NUM, String.valueOf(0L));
        kv.put(MIGRATION_ID + MigrationServiceImpl.PROGRESS_TOTAL, String.valueOf(-1L));
    }

    @Override
    public void afterRun(FeaturesRunner runner) {
        KeyValueStore kv = Framework.getService(KeyValueService.class)
                                    .getKeyValueStore(MigrationServiceImpl.KEYVALUE_STORE_NAME);
        kv.put(MIGRATION_ID + MigrationServiceImpl.STEP, (String) null);
        kv.put(MIGRATION_ID + MigrationServiceImpl.START_TIME, (String) null);
        kv.put(MIGRATION_ID + MigrationServiceImpl.PING_TIME, (String) null);
        kv.put(MIGRATION_ID + MigrationServiceImpl.PROGRESS_NUM, (String) null);
        kv.put(MIGRATION_ID + MigrationServiceImpl.PROGRESS_TOTAL, (String) null);
    }

    @RunWith(FeaturesRunner.class)
    @Features({ PropertyCommentFeature.class, BridgeCommentFeature.class })
    public static class TestBridgeCommentFeature {

        @Inject
        protected CommentManager bridge;

        @Inject
        @Named("first")
        protected CommentManager first;

        @Inject
        @Named("second")
        protected CommentManager second;

        @Test
        public void testCommentManagers() {
            assertTrue(bridge instanceof BridgeCommentManager);
            assertTrue(first instanceof PropertyCommentManager);
            assertTrue(second instanceof TreeCommentManager);
        }
    }
}
