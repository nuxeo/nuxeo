/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.marshaling;

/**
 * Base class for XML marshalers.
 *
 * @author tiry
 */
public abstract class AbstractDefaultXMLMarshaler {

    protected static final String publisherSerializerNS = "http://www.nuxeo.org/publisher";

    protected static final String publisherSerializerNSPrefix = "nxpub";

    protected String cleanUpXml(String data) {
        if (data == null) {
            return null;
        }
        if (data.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")) {
            data = data.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n",
                    "");
        }
        return data;
    }

}
