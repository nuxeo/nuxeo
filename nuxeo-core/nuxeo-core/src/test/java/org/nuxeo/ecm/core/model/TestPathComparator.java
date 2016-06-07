/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;

public class TestPathComparator {

    private static final PathComparator PATH_COMPARATOR = new PathComparator();

    @Test
    public void testBasics() throws Exception {
        Document doc1 = mock(Document.class, "doc1");
        when(doc1.getPath()).thenReturn("/doc1");
        Document doc2 = mock(Document.class, "doc2");
        when(doc2.getPath()).thenReturn("/doc2");

        Document[] array = new Document[] { doc2, doc1 };
        Arrays.sort(array, PATH_COMPARATOR);

        assertEquals("/doc1", array[0].getPath());
        assertEquals("/doc2", array[1].getPath());
    }

    @Test
    public void testNullFirst() throws Exception {
        Document doc1 = mock(Document.class, "doc1");
        when(doc1.getPath()).thenReturn("/doc1");
        Document doc2 = mock(Document.class, "doc2");
        when(doc2.getPath()).thenReturn(null);

        Document[] array = new Document[] { doc2, doc1 };
        Arrays.sort(array, PATH_COMPARATOR);

        assertNull(array[0].getPath());
        assertEquals("/doc1", array[1].getPath());
    }

    @Test
    public void testBothNull() throws Exception {
        Document doc1 = mock(Document.class, "doc1");
        when(doc1.getPath()).thenReturn(null);
        when(doc1.getUUID()).thenReturn("id1");
        Document doc2 = mock(Document.class, "doc2");
        when(doc2.getPath()).thenReturn(null);
        when(doc2.getUUID()).thenReturn("id2");

        Document[] array = new Document[] { doc2, doc1 };
        Arrays.sort(array, PATH_COMPARATOR);

        assertEquals("id1", array[0].getUUID());
        assertEquals("id2", array[1].getUUID());
    }

}
