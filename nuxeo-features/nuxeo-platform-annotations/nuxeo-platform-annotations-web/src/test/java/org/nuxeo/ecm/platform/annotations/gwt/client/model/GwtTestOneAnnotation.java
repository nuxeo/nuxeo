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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.model;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * @author Alexandre Russel
 *
 */
public class GwtTestOneAnnotation extends GWTTestCase {
    private Annotation annotation = new Annotation();

    @Override
    public String getModuleName() {
        return "org.nuxeo.ecm.platform.annotations.gwt.AnnotationPanel";
    }

    public void testGetLocalDate() {
        annotation.setStringDate("1999-10-14T12:10Z");
        String result = annotation.getFormattedDate();
        assertNotNull(result);
    }

}
