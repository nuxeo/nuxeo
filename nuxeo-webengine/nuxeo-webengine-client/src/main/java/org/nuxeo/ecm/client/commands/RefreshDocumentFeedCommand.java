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
 *     matic
 */
package org.nuxeo.ecm.client.commands;

import java.net.URL;

import org.nuxeo.ecm.client.DocumentFeed;
import org.nuxeo.ecm.client.DocumentList;

/**
 * @author matic
 *
 */
public class RefreshDocumentFeedCommand extends AbstractCommand<DocumentFeed> {

    protected String href;
    
    protected DocumentList lastEntries;
    
    public RefreshDocumentFeedCommand(String href, String serverTag, DocumentList lastEntries) {
        super("nuxeo", "refreshDocumentFeed");
        this.href = href;
        this.serverTag = serverTag;
        this.lastEntries = lastEntries;
    }
    
    @Override
    public String formatURL(URL baseURL) {
        return href;
    }
    
    public DocumentList getLastEntries() {
        return lastEntries;
    }

}
