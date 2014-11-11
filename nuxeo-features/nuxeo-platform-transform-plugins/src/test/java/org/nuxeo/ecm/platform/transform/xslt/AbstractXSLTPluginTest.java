/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.xslt;

import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public abstract class AbstractXSLTPluginTest extends AbstractPluginTestCase {

    public static final String PLUGIN_NAME = "xslt";

    public static boolean compareXML(final InputStream actual, final InputStream expected)
            throws Exception {
        final SAXReader reader = new SAXReader();

        // make sure the XML parser doesn't do any network connection by
        // avoiding validation
        reader.setFeature("http://xml.org/sax/features/validation", false);
        // reader.setFeature("http://xml.org/sax/features/external-general-entities",
        // false);
        // reader.setFeature("http://xml.org/sax/features/external-parameter-entities",
        // false);

        // xerces-specific feature that avoids loading DTDs in non-validating
        // mode
        reader.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false);

        final Document actualXml = reader.read(actual);
        final Document expectedXml = reader.read(expected);

        return actualXml.asXML().equals(expectedXml.asXML());
    }
}
