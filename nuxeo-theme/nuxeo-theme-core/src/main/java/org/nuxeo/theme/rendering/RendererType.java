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

package org.nuxeo.theme.rendering;

import java.util.ArrayList;
import java.util.Collection;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("renderer")
public final class RendererType implements Type {

    @XNode("@element")
    public String name;

    @XNodeList(value = "filter", type = ArrayList.class, componentType = String.class)
    public Collection<String> filters;

    public String getTypeName() {
        return name;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.RENDERER;
    }

    public Collection<String> getFilters() {
        return filters;
    }

}
