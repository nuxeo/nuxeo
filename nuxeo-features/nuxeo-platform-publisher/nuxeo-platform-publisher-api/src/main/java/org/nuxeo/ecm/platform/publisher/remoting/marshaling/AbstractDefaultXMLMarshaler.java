/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            data = data.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "");
        }
        return data;
    }

}
