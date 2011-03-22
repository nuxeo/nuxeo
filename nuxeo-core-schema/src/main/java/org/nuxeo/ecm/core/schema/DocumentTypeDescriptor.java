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

    @XNodeList(value = "subtypes/type", type = String[].class, componentType = String.class)
    public String[] childrenTypes;

    @XNode("prefetch")
    public String prefetch;

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

}
