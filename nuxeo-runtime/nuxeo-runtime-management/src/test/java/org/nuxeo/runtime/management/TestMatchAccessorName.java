/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    Stephane Lacoin (Nuxeo EP Software Engineer)ne Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * @authorStephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class TestMatchAccessorName extends TestCase {

    private static final Pattern fixAttributePattern = Pattern.compile("(get|set|is)(.*)");

    public void testMatchGet() {
        Matcher matcher = fixAttributePattern.matcher("getSomething");
        assertTrue(matcher.matches());
        assertEquals("Something",matcher.group(2));
    }
}
