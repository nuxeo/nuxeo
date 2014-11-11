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
package org.nuxeo.ecm.cmis.app.feeds;

import org.nuxeo.ecm.cmis.ContentManagerException;
import org.nuxeo.ecm.cmis.Session;
import org.nuxeo.ecm.cmis.client.app.APPContentManager;
import org.nuxeo.ecm.cmis.client.app.APPSession;
import org.nuxeo.ecm.cmis.client.app.Feed;
import org.nuxeo.ecm.cmis.client.app.Request;
import org.nuxeo.ecm.cmis.client.app.Response;
import org.nuxeo.ecm.cmis.common.AdapterFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class APPFeedService implements FeedService {

    protected APPSession session;

    public static void install(APPContentManager cm) {
        cm.registerAdapters(Session.class, new AdapterFactory() {
            public Class<?>[] getAdapterTypes() {
                return new Class<?>[] { FeedService.class };
            }
            public <T> T getAdapter(Object obj, Class<T> adapter) {
                return (T)new APPFeedService((APPSession)obj);
            }
        });
    }

    public APPFeedService(APPSession session) {
        this.session = session;
    }

    public APPSession getSession() {
        return session;
    }


    public Feed<FeedDescriptor> getFeeds() throws ContentManagerException {
        Request req = new Request(session.getBaseUrl()+"/feeds"); // TODO use atom collections
        Response resp = session.getConnector().get(req);
        return resp.getFeed(this, FeedDescriptor.class);
    }

}
