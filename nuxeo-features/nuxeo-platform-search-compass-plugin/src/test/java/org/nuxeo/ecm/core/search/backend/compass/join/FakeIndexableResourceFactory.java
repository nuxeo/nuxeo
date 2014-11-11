/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.backend.compass.join;

import java.io.Serializable;

import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.AbstractIndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;

/**
 * A fake factory for join logic tests.
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
@SuppressWarnings("serial")
public class FakeIndexableResourceFactory implements IndexableResourceFactory {

    private static class FakeIndexableResource extends AbstractIndexableResource {
        private static final long serialVersionUID = -918113431681470085L;

        FakeIndexableResource(String name, IndexableResourceConf conf) {
            super(name, conf);
        }

        FakeIndexableResource() {
        }

        public String computeId() {
            return null;
        }

        public Serializable getValueFor(String indexableDataName)
                throws IndexingException {
            return null;
        }
    }

    public IndexableResource createEmptyIndexableResource() {
        return new FakeIndexableResource();
    }

    public IndexableResource createIndexableResourceFrom(
            Serializable targetResource, IndexableResourceConf conf, String sid)
            throws IndexingException {
        return new FakeIndexableResource(conf.getName(), conf);
    }

    public ResolvedResource createResolvedResourceFor(
            Serializable targetResource, IndexableResourceConf conf, String sid)
            throws IndexingException {
        return null;
    }

    public ResolvedResources createResolvedResourcesFor(
            Serializable targetResource, IndexableResourceConf conf, String sid)
            throws IndexingException {
        return null;
    }

    public ResolvedResource resolveResourceFor(IndexableResource resource)
            throws IndexingException {
        return null;
    }

    public ResolvedResource resolveResourceFor(Serializable targetResource,
            IndexableResourceConf conf, String sid) throws IndexingException {
        return null;
    }

    public ResolvedResources resolveResourcesFor(IndexableResource resource)
            throws IndexingException {
        return null;
    }

}
