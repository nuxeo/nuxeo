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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
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
@RunWith(Suite.class)
@Suite.SuiteClasses({
    GwtTestRDFParser.class,
    GwtTestStatement.class,
    GwtTestOneAnnotation.class,
    GwtTestCSSClassManager.class,
    GwtTestImageRangeXPointer.class,
    GwtTestStringRangeXPointer.class,
    GwtTestXPathUtil.class
})
public class GwtTestSuiteAnnotation extends GWTTestSuite {
}

    /*GwtTestDecoratorVisitor.class,
    GwtTestPortAmsterdamParsing.class,
    GwtTestSimpleParsing.class,
    GwtTestSimpleParsingWithEntities.class,
    GwtTestTextGrabberVisitor.class*/
