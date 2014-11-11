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
package org.nuxeo.ecm.webengine.cmis.util;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.chemistry.atompub.CMIS;
import org.nuxeo.ecm.webengine.atom.XMLWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractFeedAdapter<T> implements FeedAdapter<T> {

    public void writeFeed(Request req, Iterable<T> feed, OutputStream out) throws Exception {
        XMLWriter xw = new XMLWriter(new OutputStreamWriter(out));
        setNamespaces(xw);
        xw.start();
        writeFeed(feed, xw);
        xw.end();
        xw.flush();
    }
    
    public void setNamespaces(XMLWriter xw) throws Exception {
        xw.putXmlns("http://www.w3.org/2005/Atom");
        xw.putXmlns(CMIS.CMIS_PREFIX, CMIS.CMIS_NS);        
    }
    
    public void writeFeed(Iterable<T> feed, XMLWriter xw) throws Exception {
        xw.element("feed").start();
        writeFeedHeader(xw);
        for (T entry : feed) {
            writeEntry(entry, xw);
        }
        xw.end();        
    }
    
    public void writeFeedHeader(XMLWriter xw) throws Exception {
        
    }

    public void writeEntry(T entry, XMLWriter xw) throws Exception {
        xw.element("entry").start();
        writeEntryHeader(entry, xw);
        writeCmisObject(entry, xw);
        xw.end();
    }
    
    public void writeEntryHeader(T entry, XMLWriter xw) throws Exception {

    }

    public void writeCmisObject(T entry, XMLWriter xw) throws Exception {
        
    }

}
