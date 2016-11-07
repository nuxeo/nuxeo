/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.ftest.cap;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * IT suite of CAP distribution tests for inclusion in other suites
 *
 * @since 6.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ITActivityDashboardsTest.class, ITAdminNavigationTest.class, ITArchivedVersionsTest.class,
        ITBlobActionsTest.class, ITCollectionsTest.class, ITCommentTest.class, ITContentViewLocalConfigTest.class,
        ITContextualActionsTest.class, ITCopyPasteTest.class, ITDefaultWorkflowTest.class, ITDnDImportTest.class,
        ITDocumentRelationTest.class, ITDomainTest.class, ITErrorTest.class, ITExportTest.class, ITFileUploadTest.class,
        ITFolderTest.class, ITGroupsTest.class, ITLockTest.class, ITLoginLogoutTest.class, ITLogsViewerTest.class,
        ITMainTabsTest.class, ITManageTest.class, ITMiscLittleThingsTest.class, ITModifyWorkspaceDescriptionTest.class,
        ITNoteDocumentTest.class, ITPermissionsTest.class, ITPersonalWorkspaceTest.class, ITPublishDocumentTests.class,
        ITRichfileUploadTest.class, ITRSSAtomExportTest.class, ITSafeEditTest.class, ITSearchTabTest.class,
        ITSectionTest.class, ITSelect2Test.class, ITSelectAndFilterTest.class, ITSuggestBoxTest.class,
        ITSelect2Test.class, ITSelectAndFilterTest.class, ITTaggingTest.class, ITUsersTest.class,
        ITUserProfileTest.class, ITUsersGroupsTest.class, ITUsersTest.class, ITVerifyDeleteDocumentContentTest.class,
        ITVocabularyTest.class, ITWorkListTest.class, ITWorkspaceTest.class })
public abstract class AbstractCapITSuite {

}
