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
package org.nuxeo.ecm.webengine.cmis.adapters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Person;
import org.apache.abdera.model.Text;
import org.apache.abdera.protocol.server.ProviderHelper;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.context.AbstractResponseContext;
import org.apache.abdera.protocol.server.context.BaseResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.ReturnVersion;
import org.apache.chemistry.SPI;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.atompub.ObjectElement;
import org.apache.chemistry.property.Property;
import org.apache.chemistry.repository.Repository;
import org.apache.chemistry.type.BaseType;

/**
 * A parameterized collection that is applied to a target folder object.
 * The target folder ID is retrieved from context by using the <code>objectid</code> parameter.
 * 
 * This is an abstract implementation which is handling all the details of adapting objects to feed entries. 
 * It can be used as a base by any collection that has a root folder and output a feed of objects 
 * (like children or descendant collection) 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ObjectRootedCollection extends CMISCollection<ObjectEntry> {

    
    public ObjectRootedCollection(String name, String title, Repository repository) {
        super (name, title, repository);
    }
    
    public String getObjectId(RequestContext request) {
        return request.getParameter("objectid");
    }

    @Override
    public String getHref(RequestContext request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("collection", name);
        params.put("id", getObjectId(request)); // id must not be null
        return request.absoluteUrlFor(TargetType.TYPE_COLLECTION, params);
    }

    @Override
    public String getId(RequestContext request) {
        return "urn:x-"+name+":" + getObjectId(request); 
    }

    /**
     * Used to resolve real objects. So this will be the object ID.
     */
    @Override
    public String getResourceName(RequestContext request) {
        return request.getTarget().getParameter("objectid");
    }

    @Override
    protected String getLink(ObjectEntry object, IRI feedIri,
            RequestContext request) {
        return getObjectLink(object.getId(), request);
    }
    
    /**
     * The method is no more used since {@link #getLink(String, ObjectEntry, IRI, RequestContext) was redefined
     */
    @Override
    public String getName(ObjectEntry object) {
        throw new UnsupportedOperationException(); // unused
    }
    
    /**
     * Use object ID to generate UUID
     */
    @Override
    public String getId(ObjectEntry object) {
        return "urn:uuid:" + object.getId();
    }

    @Override
    public ObjectEntry getEntry(String id, RequestContext request)
            throws ResponseContextException {
        SPI spi = getConnection(request).getSPI();
        return spi.getProperties(id, ReturnVersion.THIS, null, false, false);
    }

    
    @Override
    protected Feed createFeedBase(RequestContext request) throws ResponseContextException {
        Feed feed = super.createFeedBase(request);
        
        // RFC 5005 paging
    
        return feed;
    }

    @Override
    protected void createFeedLinks(Feed feed, RequestContext request) throws ResponseContextException {
        String objectId = getObjectId(request);
        feed.addLink(getObjectLink(objectId, request), CMIS.LINK_SOURCE);
    }    
    

    @Override
    protected ResponseContext buildGetEntryResponse(RequestContext request,
            Entry entry) throws ResponseContextException {
        Document<Entry> entryDoc = entry.getDocument();
        AbstractResponseContext rc = new BaseResponseContext<Document<Entry>>(
                entryDoc);
        rc.setEntityTag(ProviderHelper.calculateEntityTag(entry));
        return rc;
    }
    
    @Override
    protected String addEntryDetails(RequestContext request, Entry entry,
            IRI feedIri, ObjectEntry object) throws ResponseContextException {
        Factory factory = request.getAbdera().getFactory();
    
        entry.declareNS(CMIS.CMIS_NS, CMIS.CMIS_PREFIX);
    
        entry.setId(getId(object));
        entry.setTitle(getTitle(object));
        entry.setUpdated(getUpdated(object));
        List<Person> authors = getAuthors(object, request);
        if (authors != null) {
            for (Person a : authors) {
                entry.addAuthor(a);
            }
        }
        Text t = getSummary(object, request);
        if (t != null) {
            entry.setSummaryElement(t);
        }
    
        String link = getLink(object, feedIri, request);
        entry.addLink(link, "self");
        entry.addLink(link, "edit");
        // alternate is mandated by Atom when there is no atom:content
        entry.addLink(link, "alternate");
        // CMIS links
        entry.addLink(getRepositoryLink(request), CMIS.LINK_REPOSITORY);
        entry.addLink(getTypeLink(object.getTypeId(), request), CMIS.LINK_TYPE);
        if (object.getType().getBaseType() == BaseType.FOLDER) {
            String oid = object.getId();
            entry.addLink(getChildrenLink(oid, request), CMIS.LINK_CHILDREN);
            entry.addLink(getDescendantsLink(oid, request),
                    CMIS.LINK_DESCENDANTS);
            entry.addLink(getParentsLink(oid, request), CMIS.LINK_PARENTS);
            String pid = object.getId(Property.PARENT_ID);
            if (pid != null) {
                // TODO unclear in spec (parent vs parents)
                entry.addLink(getObjectLink(pid, request), CMIS.LINK_PARENT);
            }
        }
        // entry.addLink("XXX", CMIS.LINK_ALLOWABLE_ACTIONS);
        // entry.addLink("XXX", CMIS.LINK_RELATIONSHIPS);
    
        addEextensions(request, factory, entry, object);
    
        return link;
    }
    
    protected void addEextensions(RequestContext request, Factory factory, Entry entry, ObjectEntry object) throws ResponseContextException {
        // ContentStreamUri needs to know the media link
        String mediaLink = isMediaEntry(object) ? getMediaLink(object.getId(),
                request) : null;
        entry.addExtension(new ObjectElement(factory, object, mediaLink));   
    }

    @Override
    public List<Person> getAuthors(ObjectEntry object, RequestContext request) {
        String author = null;
        try {
            author = object.getString(Property.CREATED_BY);
        } catch (Exception e) {
            // no such property or bad type
        }
        if (author == null) {
            author = "system";
        }
        Person person = request.getAbdera().getFactory().newAuthor();
        person.setName(author);
        return Collections.singletonList(person);
    }
    
    @Override
    public InputStream getMediaStream(ObjectEntry object)
            throws ResponseContextException {
        // TODO entry was fetched for mostly nothing...
        SPI spi = repository.getConnection(null).getSPI();
        try {
            return spi.getContentStream(object.getId(), 0, -1);
        } catch (IOException e) {
            throw new ResponseContextException(500, e);
        }
    }

    
    @Override
    public String getTitle(ObjectEntry object) {
        String title = null;
        try {
            title = object.getString("title"); // TODO improve
        } catch (Exception e) {
            // no such property or bad type
        }
        if (title == null) {
            title = object.getName();
        }
        return title;
    }

    @Override
    public Date getUpdated(ObjectEntry object) {
        Date date = null;
        try {
            Calendar calendar = object.getDateTime(Property.LAST_MODIFICATION_DATE);
            if (calendar != null) {
                date = calendar.getTime();
            }
        } catch (Exception e) {
            // no such property or bad type
        }
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    @Override
    public Text getSummary(ObjectEntry object, RequestContext request) {
        String summary = null;
        try {
            summary = object.getString("description"); // TODO improve
        } catch (Exception e) {
            // no such property or bad type
        }
        if (summary == null) {
            return null;
        }
        Text text = request.getAbdera().getFactory().newSummary();
        text.setValue(summary);
        return text;
    }

    @Override
    protected String addMediaContent(IRI feedIri, Entry entry,
            ObjectEntry object, RequestContext request)
            throws ResponseContextException {
        String mediaLink = getMediaLink(object.getId(), request);
        entry.setContent(new IRI(mediaLink), getContentType(object));
        entry.addLink(mediaLink, "edit-media");
        entry.addLink(mediaLink, "cmis-stream");
        return mediaLink;
    }

    @Override
    public boolean isMediaEntry(ObjectEntry object)
            throws ResponseContextException {
        return object.hasContentStream();
    }
    
    // called when this is not a media entry
    @Override
    public Object getContent(ObjectEntry object, RequestContext request)
            throws ResponseContextException {
        return null;
    }

    @Override
    public String getContentType(ObjectEntry object) {
        return object.getString(Property.CONTENT_STREAM_MIME_TYPE);
    }
}
