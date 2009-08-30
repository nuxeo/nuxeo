/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("preview")
public class PreviewType implements Type {

    @XNode("@category")
    protected String category;

    @XNode("class")
    protected String className;

    @XNode("properties")
    protected String properties = "";

    public TypeFamily getTypeFamily() {
        return TypeFamily.PREVIEW;
    }

    public String getTypeName() {
        return category;
    }

    public PreviewType() {
    }

    public PreviewType(String category, String className, String properties) {
        this.category = category;
        this.className = className;
        this.properties = properties;
    }

    public String getCategory() {
        return category;
    }

    public String getClassName() {
        return className;
    }

    public String getProperties() {
        return properties;
    }

}
