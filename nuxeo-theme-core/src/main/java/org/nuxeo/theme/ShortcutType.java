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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("shortcut")
public final class ShortcutType implements Type {

    @XNode("@key")
    private String key;

    private String target;

    public TypeFamily getTypeFamily() {
        return TypeFamily.SHORTCUT;
    }

    public String getTypeName() {
        return key;
    }

    public String getKey() {
        return key;
    }

    public String getTarget() {
        return target;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    @XNode("@target")
    public void setTarget(final String target) {
        this.target = Framework.expandVars(target);
    }

}
