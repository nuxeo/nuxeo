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

package org.nuxeo.theme.test.configuration;

import java.net.URL;

import junit.framework.TestCase;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.theme.perspectives.PerspectiveType;

public class TestPerspectiveConfiguration extends TestCase {

    public void testPerspectiveType() throws Exception {
        XMap xmap = new XMap();
        xmap.register(PerspectiveType.class);

        URL url = Thread.currentThread().getContextClassLoader().getResource(
                "perspective-xmap.xml");

        PerspectiveType perspective = (PerspectiveType) xmap.load(url);
        assertEquals("view", perspective.getTypeName());
        assertEquals("View mode", perspective.getTitle());
    }

}
