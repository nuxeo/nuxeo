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
package org.nuxeo.ecm.webengine.cmis.services;

import java.util.Arrays;
import java.util.Date;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.repository.Repository;
import org.nuxeo.ecm.webengine.abdera.AbderaRequest;
import org.nuxeo.ecm.webengine.cmis.adapters.EmptyCollection;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServicesCollection extends EmptyCollection<ServiceDescriptor> {

    public ServicesCollection(String name, Repository repository) {
        super (name, "Nuxeo Services", repository);
    }
    
    @Override
    protected String getLink(ServiceDescriptor object, IRI feedIri,
            RequestContext request) {
        String url = object.getUrl();
        if (url == null) {
            return ((AbderaRequest)request).getContext().getUrlPath()+"/"+object.getId();
        } else if (url.startsWith("/")) {
            return ((AbderaRequest)request).getContext().getBasePath() + url;
        } else if (url.indexOf(":/") > -1) {
            return url;
        } else {
            return ((AbderaRequest)request).getContext().getUrlPath()+"/"+url;
        }
    }
    
    /**
     * The method is no more used since {@link #getLink(String, ObjectEntry, IRI, RequestContext) was redefined
     */
    @Override
    public String getName(ServiceDescriptor object) {
        throw new UnsupportedOperationException(); // unused
    }
    
    @Override
    public Iterable<ServiceDescriptor> getEntries(RequestContext request)
            throws ResponseContextException {
        return Arrays.asList(ServiceManager.getInstance().getServiceFactories());
    }

    @Override
    public Object getContent(ServiceDescriptor entry, RequestContext request)
            throws ResponseContextException {
        return entry.getDescription();
    }


    @Override
    public String getId(ServiceDescriptor entry) throws ResponseContextException {
        return "urn:service:"+entry.getId();
    }  

    @Override
    public String getTitle(ServiceDescriptor entry) throws ResponseContextException {
        return entry.getTitle();
    }

    @Override
    public Date getUpdated(ServiceDescriptor entry) throws ResponseContextException {
        return entry.getDateUpdated();
    }


}
