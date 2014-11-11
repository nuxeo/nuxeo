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

import java.util.List;

import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.theme.perspectives.PerspectiveType;

public interface Fragment extends Element {

    Model getModel() throws ModelException;

    void setFragmentType(FragmentType fragmentType);

    FragmentType getFragmentType();

    boolean isVisibleInPerspective(PerspectiveType perspective);

    void setVisibleInPerspective(PerspectiveType perspective);

    void setAlwaysVisible();

    List<PerspectiveType> getVisibilityPerspectives();

    boolean isDynamic();

}
