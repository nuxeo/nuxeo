/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Document Type Descriptor.
 * <p>
 * Can be used to delay document type registration when not all prerequisites are met (e.g. supertype was not yet
 * registered).
 * <p>
 * In this case the descriptor containing all the information needed to register the document is put in a queue waiting
 * for the prerequisites to be met.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("doctype")
public class DocumentTypeDescriptor {

    @XNode("@name")
    public String name;

    @XNodeList(value = "schema", type = SchemaDescriptor[].class, componentType = SchemaDescriptor.class)
    public SchemaDescriptor[] schemas;

    @XNode("@extends")
    public String superTypeName;

    @XNodeList(value = "facet@name", type = String[].class, componentType = String.class)
    public String[] facets;

    @XNode("prefetch")
    public String prefetch;

    @XNode("@append")
    public boolean append = false;

    @XNodeList(value = "subtypes/type", type = String[].class, componentType = String.class)
    public String[] subtypes = new String[0];

    @XNodeList(value = "subtypes-forbidden/type", type = String[].class, componentType = String.class)
    public String[] forbiddenSubtypes = new String[0];

    public DocumentTypeDescriptor() {
    }

    public DocumentTypeDescriptor(String superTypeName, String name, SchemaDescriptor[] schemas, String[] facets) {
        this.name = name;
        this.superTypeName = superTypeName;
        this.schemas = schemas;
        this.facets = facets;
    }

    public DocumentTypeDescriptor(String superTypeName, String name, SchemaDescriptor[] schemas, String[] facets,
            String[] subtypes, String[] forbiddenSubtypes) {
        this(superTypeName, name, schemas, facets);
        this.subtypes = subtypes;
        this.forbiddenSubtypes = forbiddenSubtypes;
    }

    @Override
    public String toString() {
        return "DocType: " + name;
    }

    public DocumentTypeDescriptor clone() {
        DocumentTypeDescriptor clone = new DocumentTypeDescriptor();
        clone.name = name;
        clone.schemas = schemas;
        clone.superTypeName = superTypeName;
        clone.facets = facets;
        clone.prefetch = prefetch;
        clone.append = append;
        clone.subtypes = subtypes;
        clone.forbiddenSubtypes = forbiddenSubtypes;
        return clone;
    }

    public DocumentTypeDescriptor merge(DocumentTypeDescriptor other) {
        // only merge schemas, facets and prefetch
        if (schemas == null) {
            schemas = other.schemas;
        } else {
            if (other.schemas != null) {
                List<SchemaDescriptor> mergedSchemas = new ArrayList<>(Arrays.asList(schemas));
                mergedSchemas.addAll(Arrays.asList(other.schemas));
                schemas = mergedSchemas.toArray(new SchemaDescriptor[mergedSchemas.size()]);
            }
        }
        if (facets == null) {
            facets = other.facets;
        } else {
            if (other.facets != null) {
                List<String> mergedFacets = new ArrayList<>(Arrays.asList(facets));
                mergedFacets.addAll(Arrays.asList(other.facets));
                facets = mergedFacets.toArray(new String[mergedFacets.size()]);
            }
        }
        if (prefetch == null) {
            prefetch = other.prefetch;
        } else {
            if (other.prefetch != null) {
                prefetch = prefetch + " " + other.prefetch;
            }
        }

        // update supertype
        if (StringUtils.isEmpty(superTypeName) && StringUtils.isNotEmpty(other.superTypeName)) {
            superTypeName = other.superTypeName;
        }

        // merge subtypes
        if (subtypes == null) {
            subtypes = other.subtypes;
        } else if (other.subtypes != null) {
            List<String> mergedTypes = new ArrayList<>(Arrays.asList(subtypes));
            mergedTypes.addAll(Arrays.asList(other.subtypes));
            subtypes = mergedTypes.toArray(new String[mergedTypes.size()]);
        }
        if (forbiddenSubtypes == null) {
            forbiddenSubtypes = other.forbiddenSubtypes;
        } else if (other.forbiddenSubtypes != null) {
            List<String> mergedTypes = new ArrayList<>(Arrays.asList(forbiddenSubtypes));
            mergedTypes.addAll(Arrays.asList(other.forbiddenSubtypes));
            forbiddenSubtypes = mergedTypes.toArray(new String[mergedTypes.size()]);
        }

        return this;
    }

}
