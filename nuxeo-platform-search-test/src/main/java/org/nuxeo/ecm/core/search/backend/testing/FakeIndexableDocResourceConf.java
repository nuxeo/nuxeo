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
 *     Nuxeo - initial API and implementation
 *
 * $Id: NXTransformExtensionPointHandler.java 18651 2007-05-13 20:28:53Z sfermigier $
 */
package org.nuxeo.ecm.core.search.backend.testing;

import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.DocumentIndexableResourceImpl;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;

/**
 * A mockup used in testing data.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
@SuppressWarnings("unchecked")
public class FakeIndexableDocResourceConf implements IndexableResourceConf {

    private static final String type = "schema";

    private static final Class klass = DocumentIndexableResourceImpl.class;

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String prefix;

    public FakeIndexableDocResourceConf(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public Map<String, IndexableResourceDataConf> getIndexableFields() {
        // Auto-generated method stub
        return null;
    }

    public String getName() {
        return name;
    }

    public boolean areAllFieldsIndexable() {
        // Auto-generated method stub
        return false;
    }

    public String getPrefix() {
        return prefix;
    }

    public Set<String> getExcludedFields() {
        // TODO Auto-generated method stub
        return null;
    }

    public static IndexableResource getIndexableResourceInstance()
            throws IndexingException {
        IndexableResource resource;

        try {
            resource = (IndexableResource) klass.newInstance();
        } catch (InstantiationException e) {
            throw new IndexingException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IndexingException(e.getMessage());
        }

        return resource;
    }

    public Class getKlass() {
        return klass;
    }

    public String getType() {
       return type;
    }

}
