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

import org.nuxeo.theme.models.Html;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.properties.FieldInfo;

public final class TextFragment extends AbstractFragment {

    @FieldInfo(type = "text area", label = "text")
    public String text = "Text here ...";

    public TextFragment() {
    }

    public TextFragment(String text) {
        this.text = text;
    }

    @Override
    public Model getModel() {
        return new Html(text);
    }

}
