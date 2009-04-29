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

package org.nuxeo.ecm.platform.annotations.gwt.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * GWT JUnit tests must extend GWTTestCase.
 */
public class GwtTestAnnotation extends GWTTestCase {

  /**
   * Must refer to a valid module that sources this class.
   */
  public String getModuleName() {
    return "org.nuxeo.ecm.platform.annotations.gwt.AnnotationPanel";
  }

  /**
   * Add as many tests as you like.
   */
  public void testSimple() {
    assertTrue(true);
  }

}
