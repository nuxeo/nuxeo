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

package org.nuxeo.theme.formats;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.relations.DefaultPredicate;
import org.nuxeo.theme.relations.Predicate;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("format")
public final class FormatType implements Type {

    @XNode("@name")
    public String name;

    @XNode("class")
    public String className;

    @XNode("predicate")
    public String predicateName;

    private Predicate predicate;

    public FormatType() {
    }

    public FormatType(String name, String predicateName, String className) {
        this.name = name;
        this.predicateName = predicateName;
        this.className = className;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.FORMAT;
    }

    public String getTypeName() {
        return name;
    }

    public String getFormatClass() {
        return className;
    }

    public Predicate getPredicate() {
        if (predicate == null) {
            predicate = new DefaultPredicate(predicateName);
        }
        return predicate;
    }

}
