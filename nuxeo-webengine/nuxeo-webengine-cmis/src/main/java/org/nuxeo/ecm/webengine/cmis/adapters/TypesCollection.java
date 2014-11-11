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
import java.util.List;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Person;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.repository.Repository;
import org.apache.chemistry.type.Type;

/**
 * CMIS Collection for the Types.
 *
 * @author Florent Guillaume
 */
public class TypesCollection extends CMISCollection<Type> {

    public TypesCollection(String title, Repository repository) {
        super("types", title, repository);
    }

    @Override
    protected String getLink(Type type, IRI feedIri, RequestContext request) {
        return getTypeLink(type.getId(), request);
    }

    /**
     * This is unused since getLink method was redefined
     */
    @Override
    public String getName(Type type) {
        throw new UnsupportedOperationException(); // unused
    }

    @Override
    public String getId(Type type) {
        return "urn:x-tid:" + type.getId();
    }


    /*
     * ----- AbstractEntityCollectionAdapter -----
     */

    @Override
    protected String addEntryDetails(RequestContext request, Entry entry,
            IRI feedIri, Type type) throws ResponseContextException {
        Factory factory = request.getAbdera().getFactory();

        entry.setId(getId(type));
        entry.setTitle(getTitle(type));
        entry.setUpdated(getUpdated(type));
        // no authors, feed has one
        String summary = type.getDescription();
        if (summary != null && summary.length() != 0) {
            entry.setSummary(summary);
        }

        String link = getLink(type, feedIri, request);
        entry.addLink(link, "self");
        entry.addLink(link, "edit");
        // alternate is mandated by Atom when there is no atom:content
        entry.addLink(link, "alternate");
        // CMIS links
        entry.addLink(getRepositoryLink(request), CMIS.LINK_REPOSITORY);

        // CMIS-specific
        // TODO refactor this to be a proper ExtensibleElement
        Element dt = factory.newElement(CMIS.DOCUMENT_TYPE, entry);
        Element el;
        // note: setText is called in a separate statement as JDK 5 has problems
        // compiling when it's on one line (compiler generics bug)
        el = factory.newElement(CMIS.TYPE_ID, dt);
        el.setText(type.getId());
        el = factory.newElement(CMIS.QUERY_NAME, dt);
        el.setText(type.getQueryName());
        el = factory.newElement(CMIS.DISPLAY_NAME, dt);
        el.setText(type.getDisplayName());
        el = factory.newElement(CMIS.BASE_TYPE, dt);
        el.setText(type.getBaseType().toString());
        el = factory.newElement(CMIS.BASE_TYPE_QUERY_NAME, dt);
        el.setText(type.getBaseTypeQueryName());
        el = factory.newElement(CMIS.PARENT_ID, dt);
        el.setText(type.getParentId());
        el = factory.newElement(CMIS.DESCRIPTION, dt);
        el.setText(type.getDescription());
        el = factory.newElement(CMIS.CREATABLE, dt);
        el.setText(bool(type.isCreatable()));
        el = factory.newElement(CMIS.FILEABLE, dt);
        el.setText(bool(type.isFileable()));
        el = factory.newElement(CMIS.QUERYABLE, dt);
        el.setText(bool(type.isQueryable()));
        el = factory.newElement(CMIS.CONTROLLABLE, dt);
        el.setText(bool(type.isControllable()));
        el = factory.newElement(CMIS.VERSIONABLE, dt);
        el.setText(bool(type.isVersionable()));

        return link;
    }



    @Override
    public Iterable<Type> getEntries(RequestContext request)
            throws ResponseContextException {
        return repository.getTypes(null, true);
    }

    @Override
    public Type postEntry(String title, IRI id, String summary, Date updated,
            List<Person> authors, Content content, RequestContext request)
            throws ResponseContextException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putEntry(Type type, String title, Date updated,
            List<Person> authors, String summary, Content content,
            RequestContext request) throws ResponseContextException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteEntry(String resourceName, RequestContext request)
            throws ResponseContextException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getContent(Type type, RequestContext request)
            throws ResponseContextException {
        return null;
    }

    @Override
    public Type getEntry(String resourceName, RequestContext request)
            throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTitle(Type type) {
        return type.getDisplayName();
    }

    @Override
    public Date getUpdated(Type type) {
        // XXX TODO mandatory field
        return new Date();
    }

}
