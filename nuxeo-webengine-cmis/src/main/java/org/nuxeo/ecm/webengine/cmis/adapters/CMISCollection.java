/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.webengine.cmis.adapters;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.abdera.protocol.server.impl.AbstractEntityCollectionAdapter;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.repository.Repository;

/**
 * Base abstract class for the CMIS collections.
 *
 * @author Florent Guillaume
 * @author Bogdan Stefanescu
 */
public abstract class CMISCollection<T> extends
        AbstractEntityCollectionAdapter<T> {

    public final static String RESOURCE_ID   = "id"; 
    public final static String RESOURCE_TYPE = "entrytype";
    

    /** Collection name - used to construct URLs **/ 
    protected final String name;

    protected final Repository repository;
    
    protected final String title;

    public CMISCollection(String name, String title, Repository repository) {
        this.name = name;
        this.repository = repository;
        this.title = title;
    }


    /*
     * ----- AbstractCollectionAdapter -----
     */
    
    @Override
    public String getId(RequestContext request) {
        return "urn:x-"+name; 
    }

    @Override
    public String getHref(RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("collection", name);
        return request.absoluteUrlFor(TargetType.TYPE_COLLECTION, params);
    }
    
    public String getTitle(RequestContext request) {
        return title;
    }
    
    @Override
    public String getAuthor(RequestContext request) {
        return "system";
    }

    //TODO: remove this?
    @Override
    public String[] getAccepts(RequestContext request) {
        return new String[0];
        // return new String[] { "application/atom+xml;type=entry" };
    }

    @Override
    protected Feed createFeedBase(RequestContext request) throws ResponseContextException {
        Factory factory = request.getAbdera().getFactory();
        Feed feed = factory.newFeed();
        feed.declareNS(CMIS.CMIS_NS, CMIS.CMIS_PREFIX);
        feed.setId(getId(request));
        feed.setTitle(getTitle(request));
        feed.addAuthor(getAuthor(request));
        feed.setUpdated(new Date()); // XXX fixed date
        createFeedLinks(feed, request);
        return feed;
    }
    
    protected void createFeedLinks(Feed feed, RequestContext request) throws ResponseContextException {
        // feed.addLink("");
        // feed.addLink("", "self");        
    }
    
    /*
     * ----- Utilities -----
     */
    
    protected static String bool(boolean bool) {
        return bool ? "true" : "false";
    }

    public String getRepositoryLink(RequestContext request) {
        return request.absoluteUrlFor(TargetType.TYPE_SERVICE, null);
    }


    public String getChildrenLink(String id, RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(RESOURCE_TYPE, "children");
        params.put(RESOURCE_ID, id);
        return request.absoluteUrlFor(TargetType.TYPE_COLLECTION, params);
    }

    public String getDescendantsLink(String id, RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(RESOURCE_TYPE, "descendants");
        params.put(RESOURCE_ID, id);
        return request.absoluteUrlFor(TargetType.TYPE_COLLECTION, params);
    }

    public String getParentsLink(String id, RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(RESOURCE_TYPE, "parents");
        params.put(RESOURCE_ID, id);
        return request.absoluteUrlFor(TargetType.TYPE_COLLECTION, params);
    }

    public String getTypeChildrenLink(String id, RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(RESOURCE_TYPE, "type_children");
        params.put(RESOURCE_ID, id);
        return request.absoluteUrlFor(TargetType.TYPE_COLLECTION, params);
    }

    public String getTypeDescendantsLink(String id, RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(RESOURCE_TYPE, "type_descendants");
        params.put(RESOURCE_ID, id);
        return request.absoluteUrlFor(TargetType.TYPE_COLLECTION, params);
    }

    public String getObjectLink(String id, RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(RESOURCE_TYPE, "object");
        params.put(RESOURCE_ID, id);
        return request.absoluteUrlFor(TargetType.TYPE_ENTRY, params);
    }

    public String getMediaLink(String id, RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(RESOURCE_TYPE, "file");
        params.put(RESOURCE_ID, id);
        return request.absoluteUrlFor(TargetType.TYPE_ENTRY, params);
    }

    public String getTypeLink(String id, RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(RESOURCE_TYPE, "type");
        params.put(RESOURCE_ID, id);
        return request.absoluteUrlFor(TargetType.TYPE_ENTRY, params);
    }

}
