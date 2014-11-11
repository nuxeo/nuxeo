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
package org.nuxeo.ecm.client.abdera;

import java.util.ArrayList;
import java.util.List;

import org.apache.abdera.model.Entry;
import org.apache.commons.collections.collection.TransformedCollection;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.QueryEntry;


/**
 * @author matic
 *
 */
public class QueryEntryTransformer implements org.apache.commons.collections.Transformer {

    protected final ContentManager contentManager;
    
    QueryEntryTransformer(ContentManager client) {
        this.contentManager = client;
    }
    public Object transform(Object input) {
        Entry atomEntry  = (Entry)input;
        return new QueryEntryAdapter(contentManager, atomEntry);
    }

    @SuppressWarnings("unchecked")
    public static List<QueryEntry> transformEntries(List<Entry> entries, ContentManager contentManager) {
        List<QueryEntry> transformedElements = new ArrayList<QueryEntry>(entries.size());
        TransformedCollection.decorate(transformedElements, new QueryEntryTransformer(contentManager)).addAll(entries);
        return transformedElements;
    }
}
