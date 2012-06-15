/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.schema.registries;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for schemas
 *
 * @since 5.6
 */
public class SchemaRegistry extends ContributionFragmentRegistry<Schema> {

    protected final Map<String, Schema> schemas = new HashMap<String, Schema>();

    protected final Map<String, Schema> uri2schemaReg = new HashMap<String, Schema>();

    protected final Map<String, Schema> prefix2schemaReg = new HashMap<String, Schema>();

    @Override
    public String getContributionId(Schema contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, Schema schema,
            Schema newOrigContrib) {
        Namespace ns = schema.getNamespace();
        uri2schemaReg.put(ns.uri, schema);
        prefix2schemaReg.put(ns.prefix, schema);
        schemas.put(id, schema);
    }

    @Override
    public void contributionRemoved(String id, Schema schema) {
        Namespace ns = schema.getNamespace();
        uri2schemaReg.remove(ns.uri);
        prefix2schemaReg.remove(ns.prefix);
        schemas.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public Schema clone(Schema orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(Schema src, Schema dst) {
        throw new UnsupportedOperationException();
    }

    // custom API

    public Schema getSchema(String name) {
        return schemas.get(name);
    }

    public Schema getSchemaFromPrefix(String schemaPrefix) {
        return prefix2schemaReg.get(schemaPrefix);
    }

    public Schema getSchemaFromURI(String schemaURI) {
        return uri2schemaReg.get(schemaURI);
    }

    public Schema[] getSchemas() {
        return schemas.values().toArray(new Schema[schemas.size()]);
    }

    public int size() {
        return schemas.size();
    }

    public void clear() {
        schemas.clear();
        prefix2schemaReg.clear();
        uri2schemaReg.clear();
        contribs.clear();
    }
}
