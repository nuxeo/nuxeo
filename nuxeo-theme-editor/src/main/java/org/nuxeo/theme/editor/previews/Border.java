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

package org.nuxeo.theme.editor.previews;

import org.nuxeo.theme.Preview;

public class Border implements Preview {

    public String render(final String value) {
        return String.format("<div style=\"border:%s; margin: 7px\"></div>",
                value);
    }

}
