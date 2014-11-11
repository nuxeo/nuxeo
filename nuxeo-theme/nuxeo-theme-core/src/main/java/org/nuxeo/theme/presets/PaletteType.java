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

package org.nuxeo.theme.presets;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("palette")
public class PaletteType implements Type {

    @XNode("@name")
    private String name;

    @XNode("@src")
    private String src;

    @XNode("@category")
    private String category = "";

    public TypeFamily getTypeFamily() {
        return TypeFamily.PALETTE;
    }

    public String getCategory() {
        return category;
    }

    public String getTypeName() {
        return name;
    }

    public String getSrc() {
        return src;
    }

    public String getName() {
        return name;
    }

}
