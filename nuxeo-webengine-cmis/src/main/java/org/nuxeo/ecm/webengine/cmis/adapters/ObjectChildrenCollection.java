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

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Person;
import org.apache.abdera.parser.ParseException;
import org.apache.abdera.protocol.server.ProviderHelper;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.context.EmptyResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.chemistry.Connection;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.ReturnVersion;
import org.apache.chemistry.SPI;
import org.apache.chemistry.VersioningState;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.property.Property;
import org.apache.chemistry.repository.Repository;
import org.apache.chemistry.type.BaseType;
import org.apache.chemistry.type.Type;
import org.nuxeo.ecm.webengine.cmis.util.PropertiesParser;

/**
 * CMIS Collection for the children of a Folder.
 *
 *
 * TODO:
 *  bs - check if some methods here can be reused for descendants and if true move them to base class
 *
 * @author Florent Guillaume
 * @author Bogdan Stefanescu
 *
 */
public class ObjectChildrenCollection extends ObjectRootedCollection {

    public ObjectChildrenCollection(String name,
            Repository repository) {
        super(name, "Children", repository);
    }

    @Override
    protected void createFeedLinks(Feed feed, RequestContext request) throws ResponseContextException {
        super.createFeedLinks(feed, request);
        String objectId = getObjectId(request);
        feed.addLink(getChildrenLink(objectId, request), "self");
    }


    /** Feed operations **/

    @Override
    public Iterable<ObjectEntry> getEntries(RequestContext request)
            throws ResponseContextException {
        SPI spi = getConnection(request).getSPI();
        boolean[] hasMoreItems = new boolean[1];
        List<ObjectEntry> children = spi.getChildren(getObjectId(request), null, null, false,
                false, 0, 0, null, hasMoreItems);
        return children;
    }

    /**
     * Not used.
     */
    @Override
    public ObjectEntry postEntry(String title, IRI id, String summary,
            Date updated, List<Person> authors, Content content,
            RequestContext request) throws ResponseContextException {
        return null;
    }

    @Override
    public ResponseContext postEntry(RequestContext request) {
        try {
        Entry entry = getEntryFromRequest(request);
        if (entry != null) {
            if (!ProviderHelper.isValidEntry(entry))
                return new EmptyResponseContext(400);

            entry.setUpdated(new Date());
            // <<<-------------------- abdera code

            Connection conn = getConnection(request);
            SPI spi = conn.getSPI();

            Element element = entry.getExtension(CMIS.OBJECT);
            Element props = element.getFirstChild(CMIS.PROPERTIES);
            PropertiesParser pp = new PropertiesParser();
            Map<String, Serializable> map = pp.parse(conn.getRepository(), null, props);
            //TODO verisoning and content stream
            String typeId = (String)map.get(Property.TYPE_ID);
            Type type = conn.getRepository().getType(typeId);
            if (type == null) {
                new EmptyResponseContext(401); //TODO handle failure
            }
            String objid = null;
            if (type.getBaseType() == BaseType.FOLDER) {
                objid = spi.createFolder(typeId, map, getObjectId(request));
            } else {
                objid = spi.createDocument(typeId, map, getObjectId(request), null, VersioningState.CHECKED_OUT);
            }
            ObjectEntry entryObj = conn.getObject(objid, ReturnVersion.THIS);

            // abdera code -------------------->>>
            entry.getIdElement().setValue(getId(entryObj));

            IRI feedIri = getFeedIRI(entryObj, request);    
            String link = getLink(entryObj, feedIri, request);

            entry.addLink(link, "edit");

            return buildCreateEntryResponse(link, entry);
        } else {
            return new EmptyResponseContext(400);
        }
        } catch (ResponseContextException e) {
            return createErrorResponse(e);
        }
    }
    private IRI getFeedIRI(ObjectEntry entryObj, RequestContext request) {
        String feedIri = getFeedIriForEntry(entryObj, request);
        return new IRI(feedIri).trailingSlash();
    }
    
    @Override
    public ResponseContext putEntry(RequestContext request) {
        try {
            String id = getResourceName(request);
            ObjectEntry entryObj = getEntry(id, request);
            
            if (entryObj == null) {
              return new EmptyResponseContext(404);
            }
            
            Entry entry = getEntryFromRequest(request);
            if (entry != null) {
                if (!ProviderHelper.isValidEntry(entry)) {
                    return new EmptyResponseContext(400);
                }
                Element element = entry.getExtension(CMIS.OBJECT);
                Element props = element.getFirstChild(CMIS.PROPERTIES);
                PropertiesParser pp = new PropertiesParser();
                Connection conn = getConnection(request);
                Type type = entryObj.getType();
                Map<String,Serializable> map = pp.parse(conn.getRepository(), type, props);
                SPI spi = conn.getSPI();
                id = spi.updateProperties(entryObj.getId(), null, map);
                entryObj = getEntry(id, request);
                entry = buildEntry(entryObj, request);
                ResponseContext rc = buildGetEntryResponse(request, entry);
                rc.setStatus(204);
                //TODO set location
                //rc.setLocation(link);
                //rc.setContentLocation(rc.getLocation().toString());
                return rc;
            } else {
                return new EmptyResponseContext(400);
            }
            
          } catch (ResponseContextException e) {
            return createErrorResponse(e);
          } catch (ParseException pe) {
            return new EmptyResponseContext(415);
          } catch (ClassCastException cce) {
            return new EmptyResponseContext(415);
          } catch (Exception e) {
            //log.warn(e.getMessage(), e);
            return new EmptyResponseContext(400);
          }
    }
    
    /**
     * Not needed since we override the {@link #putEntry(RequestContext)}
     */
    @Override
    public void putEntry(ObjectEntry object, String title, Date updated,
            List<Person> authors, String summary, Content content,
            RequestContext request) throws ResponseContextException {
        // do nothing    
    }

    @Override
    public void deleteEntry(String resourceName, RequestContext request)
            throws ResponseContextException {
        String id = getObjectId(request);
        Connection conn = getConnection(request);
        SPI spi = conn.getSPI();
        ObjectEntry entry = spi.getProperties(id, ReturnVersion.THIS, null, false, false);
        if (entry != null) {
            conn.deleteObject(entry);
        }
    }


}
