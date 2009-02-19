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
package org.nuxeo.ecm.client.cm.app;

import org.nuxeo.ecm.client.cm.ContentManager;
import org.nuxeo.ecm.client.cm.DocumentEntry;
import org.nuxeo.ecm.client.cm.Repository;
import org.nuxeo.ecm.client.cm.Session;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Test {

    public static void main(String[] args) throws Exception {
        
        ContentManager cm = new APPContentManager("http://localhost:8080/cmis");
        
        Repository repo = cm.getDefaultRepository();
        Session session = repo.open();
  
        DocumentEntry entry = session.getRoot();
        
        entry =  entry.getChild("default-domain");
        
//        Document doc = entry.getDocument();
//        
//        NewsItem ni = entry.getDocument(NewsItem.class);
        
    }
    
}
