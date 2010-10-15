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

package org.nuxeo.theme.fragments;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.models.ModelType;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("fragment")
public final class FragmentType implements Type {

    @XNode("@name")
    public String name;

    @XNode("class")
    public String className;

    @XNode("model-type")
    public String modelName;

    @XNode("dynamic")
    public boolean dynamic = true;

    public FragmentType() {
    }

    public FragmentType(String name, String className, String modelName,
            boolean dynamic) {
        this.name = name;
        this.className = className;
        this.modelName = modelName;
        this.dynamic = dynamic;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.FRAGMENT;
    }

    public String getTypeName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getModelName() {
        return modelName;
    }

    public ModelType getModelType() {
        return (ModelType) Manager.getTypeRegistry().lookup(TypeFamily.MODEL,
                modelName);
    }

    public boolean isDynamic() {
        return dynamic;
    }

}
