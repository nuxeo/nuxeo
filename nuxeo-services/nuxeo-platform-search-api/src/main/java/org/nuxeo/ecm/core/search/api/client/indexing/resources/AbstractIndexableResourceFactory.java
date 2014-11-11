/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: AbstractIndexableResourceFactory.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources;

import java.io.Serializable;

import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;

/**
 * Abstract indexable resource factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractIndexableResourceFactory implements
        IndexableResourceFactory {

    private static final long serialVersionUID = 7290522094143617159L;

    public ResolvedResource resolveResourceFor(Serializable targetResource,
            IndexableResourceConf conf, String sid) throws IndexingException {
        IndexableResource ir = createIndexableResourceFrom(targetResource,
                conf, sid);
        return resolveResourceFor(ir);
    }

    public ResolvedResource createResolvedResourceFor(
            Serializable targetResource, IndexableResourceConf conf, String sid)
            throws IndexingException {
        IndexableResource ir = createIndexableResourceFrom(
                targetResource, conf, sid);
        return resolveResourceFor(ir);
    }

    public ResolvedResources createResolvedResourcesFor(
            Serializable targetResource, IndexableResourceConf conf, String sid)
            throws IndexingException {
        IndexableResource ir = createIndexableResourceFrom(
                targetResource, conf, sid);
        return resolveResourcesFor(ir);
    }

}
