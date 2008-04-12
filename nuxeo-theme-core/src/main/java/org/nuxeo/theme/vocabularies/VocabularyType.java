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

package org.nuxeo.theme.vocabularies;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("vocabulary")
public final class VocabularyType implements Type {

    @XNode("@name")
    public String name;

    @XNode("class")
    public String className;

    public VocabularyType() {
    }

    public VocabularyType(String name, String className) {
        this.name = name;
        this.className = className;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.VOCABULARY;
    }

    public String getTypeName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

}
