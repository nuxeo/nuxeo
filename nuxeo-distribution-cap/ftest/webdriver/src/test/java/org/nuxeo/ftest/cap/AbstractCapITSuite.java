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
 *     Julien Carsique
 *
 */

package org.nuxeo.ftest.cap;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * IT suite of CAP distribution tests for inclusion in other suites
 *
 * @since 5.9.6
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ITArchivedVersionsTest.class, ITCollectionsTest.class,
        ITContextualActionsTest.class, ITCopyPasteTest.class,
        ITDefaultWorkflowTest.class, ITDnDImportTest.class,
        ITDocumentRelationTest.class, ITFileUploadTest.class,
        ITLoginLogoutTest.class, ITLogsViewerTest.class,
        ITModifyWorkspaceDescriptionTest.class, ITNoteDocumentTest.class,
        ITRichfileUploadTest.class, ITRSSAtomExportTest.class,
        ITSafeEditTest.class, ITSearchTest.class, ITSelect2Test.class,
        ITSelectAndFilterTest.class, ITTaggingTest.class, ITUsersTest.class,
        ITVocabularyTest.class })
public abstract class AbstractCapITSuite {

}
