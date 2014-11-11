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

import java.io.File;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("schema")
public class SchemaBindingDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@src")
    public String src;

    public File file;

    @XNode("@prefix")
    public String prefix = "";

    @XNode("@override")
    public boolean override = false;

    @XNode("@xsdRootElement")
    public String xsdRootElement;

    // this is set by the type service to the context that knows how to locate
    // the schema file
    public RuntimeContext context;

    public SchemaBindingDescriptor() {
    }

    public SchemaBindingDescriptor(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    @Override
    public String toString() {
        return "Schema: " + name;
    }

}
