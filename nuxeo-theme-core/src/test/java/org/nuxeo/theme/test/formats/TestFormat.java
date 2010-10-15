/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.formats;

import junit.framework.TestCase;

import org.nuxeo.theme.formats.DefaultFormat;
import org.nuxeo.theme.formats.Format;

public class TestFormat extends TestCase {

    public void testFormat() {
        Format format = new DefaultFormat();
        format.setUid(1);
        assertEquals(1, (int) format.getUid());

        assertEquals("1", format.hash());
    }

}
