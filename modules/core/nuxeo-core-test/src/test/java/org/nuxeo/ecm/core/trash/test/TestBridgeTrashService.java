/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.trash.test;

import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_ID;
import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_STEP_LIFECYCLE_TO_PROPERTY;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.test.MigrationFeature;
import org.nuxeo.ecm.core.trash.TrashServiceImpl;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.2
 */
@Features(MigrationFeature.class)
public class TestBridgeTrashService extends AbstractTestTrashService {

    @Inject
    protected MigrationFeature migrationFeature;

    @Inject
    protected TrashServiceImpl trashServiceImpl;

    @Override
    public void setUp() {
        super.setUp();
        migrationFeature.changeStatus(MIGRATION_ID, MIGRATION_STEP_LIFECYCLE_TO_PROPERTY);
        trashServiceImpl.invalidateTrashServiceImplementation();
    }

    @Ignore(value = "NXP-28982")
    @Override
    @Test
    public void testUntrashChildren() {
        // temporarily ignored
    }

    @Ignore(value = "NXP-29005")
    @Override
    @Test
    public void testTrashFolderContainingProxy() {
        // temporarily ignored
    }

}
