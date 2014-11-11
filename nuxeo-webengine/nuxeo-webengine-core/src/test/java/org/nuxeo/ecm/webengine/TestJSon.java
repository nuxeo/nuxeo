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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.webengine.util.JSonHelper;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestJSon extends NXRuntimeTestCase {

    FreemarkerEngine engine;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
    }

    public void testEmpty() {

    }

    /**
     * TODO: Test is not working because of life cycle that is missing ..
     */
    public void XXXtestJSon() throws Exception {
        final DocumentModelImpl doc = new DocumentModelImpl("/root/folder/wiki1", "test", "File");
        doc.addDataModel(new DataModelImpl("dublincore"));
        DocumentPart dc = doc.getPart("dublincore");
        dc.get("title").setValue("The dublincore title for doc1");
        dc.get("description").setValue("A description *with* wiki code and a WikiName");
        System.out.println(">> "+JSonHelper.toJSon(doc));
    }

}
