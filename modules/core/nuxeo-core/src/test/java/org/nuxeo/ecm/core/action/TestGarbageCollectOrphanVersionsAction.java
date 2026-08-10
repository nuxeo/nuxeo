/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.action.GarbageCollectOrphanVersionsAction.GarbageCollectOrphanVersionsComputation;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;

/**
 * Unit tests for the {@link GarbageCollectOrphanVersionsComputation} caches.
 * <p>
 * These tests exercise the computation directly (bypassing the stream/bulk pipeline) with a mocked {@link CoreSession},
 * so that the number of calls to {@link CoreSession#removeOrphanVersions} can be asserted precisely, proving the caches
 * actually prevent redundant calls within and across buckets of the same command.
 *
 * @since 2025.24
 */
public class TestGarbageCollectOrphanVersionsAction {

    protected static final String SERIES_1 = "series1";

    protected static final String SERIES_2 = "series2";

    /** Exposes the protected {@code command}/{@code delta} fields so the computation can be driven without a stream. */
    protected static class TestableComputation extends GarbageCollectOrphanVersionsComputation {

        TestableComputation(BulkCommand command) {
            setCommand(command);
        }

        void setCommand(BulkCommand bulkCommand) {
            command = bulkCommand;
            delta = BulkStatus.deltaOf(bulkCommand.getId());
        }
    }

    protected BulkCommand newCommand() {
        return new BulkCommand.Builder("garbageCollectOrphanVersions", "SELECT * FROM Document",
                "Administrator").repository("test").build();
    }

    protected DocumentModel mockVersion(String id, String versionSeriesId) {
        DocumentModel doc = mock(DocumentModel.class);
        when(doc.getId()).thenReturn(id);
        when(doc.isVersion()).thenReturn(true);
        when(doc.getVersionSeriesId()).thenReturn(versionSeriesId);
        return doc;
    }

    protected CoreSession mockSessionReturning(DocumentModel... docs) {
        CoreSession session = mock(CoreSession.class);
        DocumentModelList list = new DocumentModelListImpl(List.of(docs));
        when(session.query(anyString())).thenReturn(list);
        when(session.removeOrphanVersions(any(DocumentRef.class))).thenReturn(List.of());
        return session;
    }

    // NXP-33866: the versionSeriesIds cache was never populated, so every version of the same series triggered a
    // redundant removeOrphanVersions() call
    @Test
    public void shouldCallRemoveOrphanVersionsOnceForDocumentsOfTheSameVersionSeries() {
        DocumentModel v1 = mockVersion("v1", SERIES_1);
        DocumentModel v2 = mockVersion("v2", SERIES_1);
        CoreSession session = mockSessionReturning(v1, v2);
        TestableComputation computation = new TestableComputation(newCommand());

        computation.startBucket("bucket1");
        computation.compute(session, List.of("v1", "v2"), Map.of());

        // only one call for the whole version series, thanks to the versionSeriesIds cache
        verify(session, times(1)).removeOrphanVersions(new IdRef(SERIES_1));
    }

    // NXP-33866: the startBucket guard was inverted, clearing the caches on every bucket of the *same* command
    // instead of only when a *new* command starts, so the cache never survived across buckets
    @Test
    public void shouldKeepCachesAcrossBucketsOfTheSameCommand() {
        BulkCommand command = newCommand();
        TestableComputation computation = new TestableComputation(command);

        // first bucket: one version of series1 is processed
        DocumentModel v1 = mockVersion("v1", SERIES_1);
        CoreSession session1 = mockSessionReturning(v1);
        computation.startBucket("bucket1");
        computation.compute(session1, List.of("v1"), Map.of());
        verify(session1, times(1)).removeOrphanVersions(new IdRef(SERIES_1));

        // second bucket of the *same* command: another version of the *same* series is processed
        DocumentModel v2 = mockVersion("v2", SERIES_1);
        CoreSession session2 = mockSessionReturning(v2);
        computation.startBucket("bucket2");
        computation.compute(session2, List.of("v2"), Map.of());

        // the cache built during bucket1 must still prevent the redundant call in bucket2
        verify(session2, never()).removeOrphanVersions(any(DocumentRef.class));
    }

    // NXP-33866: caches must still be reset when a *new* bulk command starts, otherwise entries would leak across
    // unrelated bulk GC runs
    @Test
    public void shouldResetCachesWhenANewBulkCommandStarts() {
        TestableComputation computation = new TestableComputation(newCommand());

        DocumentModel v1 = mockVersion("v1", SERIES_1);
        CoreSession session1 = mockSessionReturning(v1);
        computation.startBucket("bucket1");
        computation.compute(session1, List.of("v1"), Map.of());
        verify(session1, times(1)).removeOrphanVersions(new IdRef(SERIES_1));

        // a new bulk command starts, targeting the same version series
        computation.setCommand(newCommand());
        DocumentModel v2 = mockVersion("v2", SERIES_1);
        CoreSession session2 = mockSessionReturning(v2);
        computation.startBucket("bucket1");
        computation.compute(session2, List.of("v2"), Map.of());

        // the new command must not reuse the previous command's cache
        verify(session2, times(1)).removeOrphanVersions(new IdRef(SERIES_1));
    }

    // the deletedVersions cache must also be preserved across buckets of the same command
    @Test
    public void shouldSkipAlreadyDeletedVersionAcrossBucketsOfTheSameCommand() {
        TestableComputation computation = new TestableComputation(newCommand());

        // v1 removal also deletes v2 (e.g. same series, minor version cleaned up together)
        DocumentModel v1 = mockVersion("v1", SERIES_1);
        CoreSession session1 = mockSessionReturning(v1);
        when(session1.removeOrphanVersions(new IdRef(SERIES_1))).thenReturn(List.of(new IdRef("v2")));
        computation.startBucket("bucket1");
        computation.compute(session1, List.of("v1"), Map.of());
        verify(session1, times(1)).removeOrphanVersions(new IdRef(SERIES_1));

        // v2 shows up on its own in a later bucket of the same command, in a different series
        DocumentModel v2 = mockVersion("v2", SERIES_2);
        CoreSession session2 = mockSessionReturning(v2);
        computation.startBucket("bucket2");
        computation.compute(session2, List.of("v2"), Map.of());

        // v2 is already known to be deleted, no call should be made for it
        verify(session2, never()).removeOrphanVersions(any(DocumentRef.class));
    }
}
