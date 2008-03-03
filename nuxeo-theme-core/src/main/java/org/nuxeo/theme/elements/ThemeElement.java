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

package org.nuxeo.theme.elements;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.nodes.Node;

public class ThemeElement extends AbstractElement {

    @Override
    public List<Node> getChildrenInContext(URL themeURL) {
        List<Node> children = new ArrayList<Node>();
        children.add(Manager.getThemeManager().getThemePageByUrl(themeURL));
        return children;
    }

}
