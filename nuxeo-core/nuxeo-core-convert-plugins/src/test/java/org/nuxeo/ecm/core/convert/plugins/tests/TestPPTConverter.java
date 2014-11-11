/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.plugins.tests;

public class TestPPTConverter extends BaseConverterTest {

    // PPT POI tests fails in surefire

    public void testPptConverter() throws Exception {
        doTestTextConverter("application/vnd.ms-powerpoint", "ppt2text",
                "hello.ppt");
    }

    public void testAnyToTextConverter() throws Exception {
        doTestAny2TextConverter("application/vnd.ms-powerpoint",
                    "any2text", "hello.ppt");
    }

}
