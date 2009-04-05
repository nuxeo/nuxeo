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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Person;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.chemistry.repository.Repository;

/**
 * Collection to be used as the base of static collections that doesn't depends on a contextual root object.
 *
 * @author Bogdan Stefanescu
 */
public class StaticCollection extends CMISCollection<Object> {

    public StaticCollection(String name, String title,
            Repository repository) {
        super(name, title, repository);
    }

    /*
     * ----- CollectionInfo -----
     */

    public String getTitle(RequestContext request) {
        return title;
    }

    /*
     * ----- AbstractEntityCollectionAdapter -----
     */

    @Override
    public Object postEntry(String title, IRI id, String summary, Date updated,
            List<Person> authors, Content content, RequestContext request)
            throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void putEntry(Object entry, String title, Date updated,
            List<Person> authors, String summary, Content content,
            RequestContext request) throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteEntry(String resourceName, RequestContext request)
            throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Object> getEntries(RequestContext request)
            throws ResponseContextException {
        return Collections.emptyList();
    }

    @Override
    public Object getContent(Object entry, RequestContext request)
            throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getEntry(String resourceName, RequestContext request)
            throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId(Object entry) throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName(Object entry) throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTitle(Object entry) throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getUpdated(Object entry) throws ResponseContextException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
