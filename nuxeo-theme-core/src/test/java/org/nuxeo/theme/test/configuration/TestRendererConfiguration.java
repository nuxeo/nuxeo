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
import org.nuxeo.theme.rendering.RendererType;

public class TestRendererConfiguration extends TestCase {

    public void testRendererType() throws Exception {
        XMap xmap = new XMap();
        xmap.register(RendererType.class);

        URL url = Thread.currentThread().getContextClassLoader().getResource(
                "renderer-xmap.xml");

        RendererType renderer = (RendererType) xmap.load(url);
        assertEquals("theme", renderer.getTypeName());
        assertEquals("[widget, style]", renderer.getFilters().toString());
    }

}
