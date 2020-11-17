/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

/**
 * Tests the {@link RestHelper}.
 * <p>
 * We need to have this test here in {@code nuxeo-server-tests} because {@link RestHelper} statically try to connect to
 * a remote Nuxeo server since upgrade of {@code nuxeo-java-client} to 3.0.0.
 *
 * @since 9.2
 */
public class TestRestHelper {

    @Test
    public void testDirectoryEntriesToDelete() {
        RestHelper.addDirectoryEntryToDelete("directory1", "entry1");
        RestHelper.addDirectoryEntryToDelete("directory1", "entry2");
        RestHelper.addDirectoryEntryToDelete("directory2", "entry1");
        RestHelper.addDirectoryEntryToDelete("directory2", "entry3");
        assertEquals(2, RestHelper.directoryEntryIdsToDelete.size());
        Set<String> directory1Entries = RestHelper.directoryEntryIdsToDelete.get("directory1");
        assertTrue(CollectionUtils.isEqualCollection(Arrays.asList("entry1", "entry2"), directory1Entries));
        Set<String> directory2Entries = RestHelper.directoryEntryIdsToDelete.get("directory2");
        assertTrue(CollectionUtils.isEqualCollection(Arrays.asList("entry1", "entry3"), directory2Entries));

        RestHelper.clearDirectoryEntryIdsToDelete();
        assertTrue(RestHelper.directoryEntryIdsToDelete.isEmpty());
    }

}
