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
import java.util.List;

import org.nuxeo.theme.nodes.Node;
import org.nuxeo.theme.relations.Relate;
import org.nuxeo.theme.uids.Identifiable;

public interface Element extends Relate, Node, Identifiable {

    ElementType getElementType();

    void setElementType(ElementType elementType);

    List<Node> getChildrenInContext(URL themeURL);

    void setDescription(String description);

    String getDescription();

    boolean isEmpty();

    String computeXPath();

}
