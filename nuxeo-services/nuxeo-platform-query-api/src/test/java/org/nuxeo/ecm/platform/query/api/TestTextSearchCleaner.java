/*
 * (C) Copyright 2011-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     eugen
 */
package org.nuxeo.ecm.platform.query.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestTextSearchCleaner {

    @Test
    public void testCleaner() throws Exception {
        assertEquals("= 'a'", NXQLQueryBuilder.serializeFullText("a"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a b"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText(" a b "));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a  b"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a & b"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a : b"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a | b"));

        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a { b"));

        assertEquals("= 'a b c d e f'", NXQLQueryBuilder.serializeFullText("a#b|c  d+e*f"));
        assertEquals("= '\"a b\"'", NXQLQueryBuilder.serializeFullText("\"a b\""));
        assertEquals("= '\"a b\"'", NXQLQueryBuilder.serializeFullText("\"a-b\""));
        assertEquals("= '\"a -b \"'", NXQLQueryBuilder.serializeFullText("\"a -b \""));

        assertEquals("= 'a* b'", NXQLQueryBuilder.serializeFullText("a* b-"));

        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a*b"));

        assertEquals("= 'a  b'", NXQLQueryBuilder.serializeFullText("a*-b"));

        assertEquals("= 'a -b'", NXQLQueryBuilder.serializeFullText("*a -b"));

        assertEquals("= 'a -bc*'", NXQLQueryBuilder.serializeFullText("a | -bc*"));

        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a !#$%&()*+,-./:;<=>?@^`{|}~ b"));

        // raw sanitizeFulltextInput API that does not wrap the input with the
        // quote and the predicate operator
        assertEquals("some stuff", NXQLQueryBuilder.sanitizeFulltextInput("some & stuff\\"));

        // test negative queries
        assertEquals("= 'a -b'", NXQLQueryBuilder.serializeFullText("a !#$%&()*+,-./:;<=>?@^`{|}~ -b"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.query.api.test:configuration-test-contrib.xml")
    public void testCustomCleaner() throws Exception {

        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        String s = cs.getProperty(NXQLQueryBuilder.IGNORED_CHARS_KEY);
        assertEquals("&/{}()", s);
        assertNotNull(s);
        assertEquals("= 'a $ b'", NXQLQueryBuilder.serializeFullText("a $ b"));
        assertEquals("= '10.3'", NXQLQueryBuilder.serializeFullText("10.3"));
    }

}
