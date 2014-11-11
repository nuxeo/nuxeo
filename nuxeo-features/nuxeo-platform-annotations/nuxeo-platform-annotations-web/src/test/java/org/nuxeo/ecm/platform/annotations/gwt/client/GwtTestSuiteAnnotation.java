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

package org.nuxeo.ecm.platform.annotations.gwt.client;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.nuxeo.ecm.platform.annotations.gwt.client.annotea.GwtTestRDFParser;
import org.nuxeo.ecm.platform.annotations.gwt.client.annotea.GwtTestStatement;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.GwtTestOneAnnotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.GwtTestCSSClassManager;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.GwtTestImageRangeXPointer;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.GwtTestStringRangeXPointer;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.GwtTestXPathUtil;

import com.google.gwt.junit.tools.GWTTestSuite;

/**
 * @author Alexandre Russel
 *
 */
public class GwtTestSuiteAnnotation extends GWTTestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite();
        // annotea
        suite.addTestSuite(GwtTestRDFParser.class);
        suite.addTestSuite(GwtTestStatement.class);
        // model
        suite.addTestSuite(GwtTestOneAnnotation.class);
        // util
        suite.addTestSuite(GwtTestCSSClassManager.class);
        suite.addTestSuite(GwtTestImageRangeXPointer.class);
        suite.addTestSuite(GwtTestStringRangeXPointer.class);
        suite.addTestSuite(GwtTestXPathUtil.class);
        // view
        // suite.addTestSuite(GwtTestDecoratorVisitor.class);
        // suite.addTestSuite(GwtTestPortAmsterdamParsing.class);
        // suite.addTestSuite(GwtTestSimpleParsing.class);
        // suite.addTestSuite(GwtTestSimpleParsingWithEntities.class);
        // suite.addTestSuite(GwtTestTextGrabberVisitor.class);
        return suite;
    }
}
