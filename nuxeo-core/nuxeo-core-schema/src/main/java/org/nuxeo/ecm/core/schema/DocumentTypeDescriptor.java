/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Document Type Descriptor.
 * <p>
 * Can be used to delay document type registration when not all prerequisites
 * are met (e.g. supertype was not yet registered).
 * <p>
 * In this case the descriptor containing all the information needed to register
 * the document is put in a queue waiting for the prerequisites to be met.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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

    public DocumentTypeDescriptor() {
    }

    public DocumentTypeDescriptor(String superTypeName, String name,
            SchemaDescriptor[] schemas, String[] facets) {
        this.name = name;
        this.superTypeName = superTypeName;
        this.schemas = schemas;
        this.facets = facets;
    }

    @Override
    public String toString() {
        return "DocType: "+name;
    }

    public DocumentTypeDescriptor clone() {
        DocumentTypeDescriptor clone = new DocumentTypeDescriptor();
        clone.name = name;
        clone.schemas = schemas;
        clone.superTypeName = superTypeName;
        clone.facets = facets;
        clone.prefetch = prefetch;
        clone.append = append;
        return clone;
    }

    public DocumentTypeDescriptor merge(DocumentTypeDescriptor other) {
        // only merge schemas, facets and prefetch
        if (schemas == null) {
            schemas = other.schemas;
        } else {
            if (other.schemas != null) {
                List<SchemaDescriptor> mergedSchemas = new ArrayList<SchemaDescriptor>(
                        Arrays.asList(schemas));
                mergedSchemas.addAll(Arrays.asList(other.schemas));
                schemas = mergedSchemas.toArray(new SchemaDescriptor[mergedSchemas.size()]);
            }
        }
        if (facets == null) {
            facets = other.facets;
        } else {
            if (other.facets != null) {
                List<String> mergedFacets = new ArrayList<String>(
                        Arrays.asList(facets));
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
        return this;
    }

}
