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

package org.nuxeo.theme.jsf.component;

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.ShortcutType;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class UIAccesskeys extends UIOutput {

    @Override
    public void encodeBegin(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();

        final List<Type> shortcuts = typeRegistry.getTypes(TypeFamily.SHORTCUT);
        if (shortcuts == null) {
            return;
        }

        writer.startElement("div", this);
        for (Type type : shortcuts) {
            final ShortcutType shortcut = (ShortcutType) type;
            writer.startElement("a", this);
            writer.writeAttribute("href", shortcut.getTarget(), null);
            writer.writeAttribute("accesskey", shortcut.getKey(), null);
            writer.endElement("a");
        }
    }

    @Override
    public void encodeEnd(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");
    }
}
