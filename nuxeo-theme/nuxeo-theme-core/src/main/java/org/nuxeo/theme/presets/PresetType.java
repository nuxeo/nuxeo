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

@XObject("preset")
public class PresetType implements Type {

    @XNode("@name")
    private String name;

    @XNode("@value")
    private String value;

    @XNode("@group")
    private String group = "";

    @XNode("@category")
    private String category = "";

    public PresetType() {
    }

    public PresetType(String name, String value, String group, String category) {
        this.name = name;
        this.value = value;
        this.group = group;
        this.category = category;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.PRESET;
    }

    public String getTypeName() {
        if (!group.equals("")) {
            return String.format("%s (%s)", name, group);
        }
        return name;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getCategory() {
        return category;
    }

    public String getGroup() {
        return group;
    }

}
