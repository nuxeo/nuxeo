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
package org.nuxeo.ecm.cmis.client.app;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.cmis.CmisExtensionFactory;
import org.nuxeo.ecm.cmis.DocumentEntry;
import org.nuxeo.ecm.cmis.Repository;
import org.nuxeo.ecm.cmis.Session;
import org.nuxeo.ecm.cmis.app.feeds.APPFeedService;
import org.nuxeo.ecm.cmis.app.feeds.APPFeedsHandler;
import org.nuxeo.ecm.cmis.app.feeds.FeedDescriptor;
import org.nuxeo.ecm.cmis.app.feeds.FeedService;
import org.nuxeo.ecm.cmis.client.app.abdera.APPDocumentEntryHandler;
import org.nuxeo.ecm.cmis.client.app.abdera.APPServiceDocumentHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Test {

    public static void main(String[] args) throws Exception {
        
        Abdera.getInstance().getConfiguration().addExtensionFactory(
                new CmisExtensionFactory());
        
        APPContentManager cm = new APPContentManager("http://dormeur:8081/cmis");
        cm.registerSerializationHandler(new APPServiceDocumentHandler());
        cm.registerSerializationHandler(new APPFeedsHandler());
        cm.registerSerializationHandler(new APPDocumentEntryHandler());
        APPFeedService.install(cm);
        
        Repository repo = cm.getDefaultRepository();
        Session session = repo.open();
        FeedService feedsvc = session.getService(FeedService.class);
        Feed<FeedDescriptor> feeds = feedsvc.getFeeds();
        System.out.println("Remote Feeds: ");
        for (FeedDescriptor fd : feeds.getEntries()) {
            System.out.println(fd.getTitle()+" - "+fd.getUrl());
        }
        
        FeedDescriptor fd = feeds.getEntries().get(0);               
        Feed<DocumentEntry> docs = fd.query();
        
        int i = 1;
        System.out.println("### Docs in '"+fd.getTitle()+"'");
        for (DocumentEntry entry :  docs.getEntries()) {
            System.out.println(i+". "+entry.getTitle());
            i++;
        }
        
        
        //DocumentEntry entry = session.getRoot();        
        //entry =  entry.getChild("default-domain");
        
        
        
//        Document doc = entry.getDocument();
//        
//        NewsItem ni = entry.getDocument(NewsItem.class);
        
    }
    
}
