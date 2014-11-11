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

import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 *
 */
public class TestTextSearchCleaner extends TestCase {

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
    }

}
