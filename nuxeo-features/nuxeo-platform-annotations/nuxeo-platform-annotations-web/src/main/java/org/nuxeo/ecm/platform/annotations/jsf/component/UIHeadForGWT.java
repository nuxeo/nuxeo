/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.jsf.component;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.theme.jsf.component.UIHead;

public class UIHeadForGWT extends UIHead {

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        super.encodeAll(context);
        final ResponseWriter writer = context.getResponseWriter();

        // GWT locale
        final String locale = LocaleSelector.instance().getLocaleString();
        writer.write(String.format(
                "<meta name=\"gwt:property\" content=\"locale=%s\" />", locale));
    }

}
