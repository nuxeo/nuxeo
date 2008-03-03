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

package org.nuxeo.theme.perspectives;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.relations.Relate;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("perspective")
public final class PerspectiveType implements Relate, Type {

    @XNode("@name")
    public String name;

    @XNode("title")
    public String title;

    public PerspectiveType() {
    }

    public PerspectiveType(String name, String title) {
        this.name = name;
        this.title = title;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.PERSPECTIVE;
    }

    public String getTypeName() {
        return name;
    }

    public String hash() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

}
