/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     eugen
 */
package org.nuxeo.ecm.platform.query.api;

import java.io.File;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 *
 */
public class TestTextSearchCleaner extends NXRuntimeTestCase {

    @Test
    public void testCleaner() throws Exception {
        assertEquals("= '+a'", NXQLQueryBuilder.serializeFullText("a"));
        assertEquals("= '+a +b'", NXQLQueryBuilder.serializeFullText("a b"));
        assertEquals("= '+a +b'", NXQLQueryBuilder.serializeFullText(" a b "));
        assertEquals("= '+a +b'", NXQLQueryBuilder.serializeFullText("a  b"));
        assertEquals("= '+a +b'", NXQLQueryBuilder.serializeFullText("a & b"));
        assertEquals("= '+a +b'", NXQLQueryBuilder.serializeFullText("a : b"));
        assertEquals("= '+a +b'", NXQLQueryBuilder.serializeFullText("a | b"));

        assertEquals("= '+a +b'", NXQLQueryBuilder.serializeFullText("a { b"));

        assertEquals("= '+a +b +c +d +e +f'",
                NXQLQueryBuilder.serializeFullText("a#b|c  d+e*f"));

        assertEquals(
                "= '+a +b'",
                NXQLQueryBuilder.serializeFullText("a !#$%&()*+,-./:;<=>?@^`{|}~ b"));

        // raw sanitizeFulltextInput API that does not wrap the input with the
        // quote and the predicate operator
        assertEquals("+some +stuff",
                NXQLQueryBuilder.sanitizeFulltextInput("some & stuff\\"));
    }

    @Test
    public void testCustomCleaner() throws Exception {
        File config = Environment.getDefault().getConfig();
        config.mkdirs();
        File myProps = new File(config, "extra.properties");
        FileUtils.copyToFile(
                getClass().getResourceAsStream("/extra.properties"), myProps);
        Framework.getRuntime().reloadProperties();
        String s = Framework.getProperty(NXQLQueryBuilder.IGNORED_CHARS_KEY);
        assertEquals("&/{}()", s);
        assertNotNull(s);
        assertEquals("= '+a +$ +b'",
                NXQLQueryBuilder.serializeFullText("a $ b"));

        assertEquals("= '+10.3'", NXQLQueryBuilder.serializeFullText("10.3"));
        myProps.delete();
        Framework.getRuntime().reloadProperties();

    }

}
