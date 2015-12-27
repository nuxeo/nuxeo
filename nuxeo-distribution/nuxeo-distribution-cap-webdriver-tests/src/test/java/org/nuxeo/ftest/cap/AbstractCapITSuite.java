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
@Suite.SuiteClasses({ ITSearchTabTest.class, ITArchivedVersionsTest.class, ITCollectionsTest.class,
        ITContextualActionsTest.class, ITCopyPasteTest.class, ITDefaultWorkflowTest.class, ITDnDImportTest.class,
        ITDocumentRelationTest.class, ITFileUploadTest.class, ITLoginLogoutTest.class, ITLogsViewerTest.class,
        ITModifyWorkspaceDescriptionTest.class, ITNoteDocumentTest.class, ITRichfileUploadTest.class,
        ITRSSAtomExportTest.class, ITSafeEditTest.class, ITSuggestBoxTest.class, ITSelect2Test.class,
        ITSelectAndFilterTest.class, ITTaggingTest.class, ITUsersTest.class, ITVocabularyTest.class })
public abstract class AbstractCapITSuite {

}
