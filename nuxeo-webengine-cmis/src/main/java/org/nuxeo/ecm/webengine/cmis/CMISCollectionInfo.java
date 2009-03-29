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
package org.nuxeo.ecm.webengine.cmis;

import java.io.IOException;

import org.apache.abdera.protocol.server.impl.AbstractCollectionAdapter;
import org.nuxeo.ecm.webengine.atom.CollectionInfo;
import org.nuxeo.ecm.webengine.atom.XMLWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CMISCollectionInfo extends CollectionInfo {

    protected String type;

    public CMISCollectionInfo(String id, String type, String title, AbstractCollectionAdapter adapter) {
        super (id, title, adapter);
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    @Override
    protected void writeCollectionAttributes(String baseUrl, XMLWriter xw)
            throws IOException {
        xw.attr("cmis:collectionType", type);
    } 
    
}
