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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestMarkLogicQuerySimpleBuilder extends AbstractTest {

    @Test
    public void testEq() throws Exception {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(Collections.emptyList());
        String query = builder.eq(KEY_ID, "ID").build();
        assertFileAgainstString("query-simple/query-eq.txt", query);
    }

    /*
     * NXP-21397
     */
    @Test
    public void testEqWithAmpersand() throws Exception {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(Collections.emptyList());
        String query = builder.eq(KEY_ID, "ID &").build();
        assertFileAgainstString("query-simple/query-eq-with-ampersand.txt", query);
    }

    @Test
    public void testEqWithList() throws Exception {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(Collections.emptyList());
        String query = builder.eq(KEY_ANCESTOR_IDS, "ID").build();
        assertFileAgainstString("query-simple/query-eq-with-key-list.txt", query);
    }

    @Test
    public void testEqWithRangeElementIndex() throws Exception {
        MarkLogicRangeElementIndexDescriptor desc = new MarkLogicRangeElementIndexDescriptor();
        desc.element = KEY_ID;
        desc.type = "string";
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(Collections.singletonList(desc));
        String query = builder.eq(KEY_ID, "ID").build();
        assertFileAgainstString("query-simple/query-eq-with-range-element-index.txt", query);
    }

    @Test
    public void testNotIn() throws Exception {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(Collections.emptyList());
        String query = builder.notIn(KEY_ID, Arrays.asList("ID1", "ID2")).build();
        assertFileAgainstString("query-simple/query-not-in.txt", query);
    }

    @Test
    public void testNotInOneElement() throws Exception {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(Collections.emptyList());
        String query = builder.notIn(KEY_ID, Collections.singleton("ID1")).build();
        assertFileAgainstString("query-simple/query-not-in-one-element.txt", query);
    }

}
