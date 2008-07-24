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

package org.nuxeo.theme.html.ui;

import java.util.List;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.ShortcutType;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class Accesskeys {

    public static String render() {
        StringBuilder sb = new StringBuilder();
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        final List<Type> shortcuts = typeRegistry.getTypes(TypeFamily.SHORTCUT);
        if (shortcuts == null) {
            return "";
        }
        sb.append("<div>");
        for (Type type : shortcuts) {
            final ShortcutType shortcut = (ShortcutType) type;
            sb.append(String.format("<a href=\"%s\" accesskey=\"%s\"></a>",
                    shortcut.getTarget(), shortcut.getKey()));
        }
        sb.append("</div>");
        return sb.toString();
    }

}
