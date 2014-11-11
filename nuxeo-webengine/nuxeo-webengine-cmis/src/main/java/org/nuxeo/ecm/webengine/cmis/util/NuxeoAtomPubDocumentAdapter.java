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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.ReturnVersion;
import org.apache.chemistry.SPI;
import org.apache.chemistry.VersioningState;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.property.Property;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoAtomPubDocumentAdapter extends AtomPubDocumentAdapter {

    @Override
    public Iterator<ObjectEntry> getChildren(Request req) {
        SPI spi = req.getConnection().getSPI();
        boolean[] hasMoreItems = new boolean[1];
        List<ObjectEntry> children = spi.getChildren(req.getObjectId(), null, null, false,
                false, 0, 0, null, hasMoreItems);
        return children.iterator();
    }
    
    @Override
    public boolean deleteObject(Request req) {
        ObjectEntry entry = getObject(req);
        if (entry != null) {
            req.getConnection().deleteObject(entry);
            return true;
        }
        return false;
    }

    @Override
    public Iterator<ObjectEntry> getDescendants(Request req) {
        SPI spi = req.getConnection().getSPI();
        List<ObjectEntry> children = spi.getDescendants(req.getObjectId(), null, -1, null, false, false, null);
        return children.iterator();
    }

    @Override
    public ObjectEntry getObject(Request req) {
        SPI spi = req.getConnection().getSPI();
        return spi.getProperties(req.getObjectId(), ReturnVersion.THIS, null, false, false);
    }

    @Override
    public Iterator<ObjectEntry> getParents(Request req) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ObjectEntry postEntry(Request req) throws Exception {
        Entry entry = req.getRequestEntry();
        SPI spi = req.getConnection().getSPI();
        Element element = entry.getExtension(CMIS.OBJECT);
        Element props = element.getFirstChild(CMIS.PROPERTIES);
        PropertiesParser pp = new PropertiesParser();
        Map<String, Serializable> map = pp.parse(req.getConnection().getRepository(), null, props);
        //TODO verisoning and content stream    
        String id = spi.createDocument((String)map.get(Property.TYPE_ID), map, req.getObjectId(), null, VersioningState.CHECKED_OUT);
        return req.getConnection().getObject(id, ReturnVersion.THIS);
    }

    @Override
    public ObjectEntry putEntry(Request req) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Iterator<ObjectEntry> query(Request req) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
