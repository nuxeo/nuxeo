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
 */
package org.nuxeo.ecm.cmis.client.app.stax;

import java.net.URL;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StaxParser {

    public static void main(String[] args) throws Exception {

        System.out.println(FileUtils.read(new URL("http://dormeur:8081/cmis/feeds").openStream()));
        
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(
                new URL("http://dormeur:8081/cmis/feeds").openStream()
        );
        
        while(reader.hasNext()) {
            System.out.println(">> "+reader.next());
            if (reader.getEventType()==1) System.out.println(reader.getLocalName());
            System.out.println("===============");
        }
    }
    
}
