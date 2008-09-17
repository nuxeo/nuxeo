/*
 * (C) Copyright 2006-2008 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.editor;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.theme.fragments.FragmentType;
import org.nuxeo.theme.views.ViewType;

public class FragmentInfo {

    FragmentType fragmentType;

    public List<ViewType> getViewTypes() {
        return viewTypes;
    }

    FragmentInfo(FragmentType fragmentType) {
        this.fragmentType = fragmentType;
    }

    FragmentType getFragmentType() {
        return fragmentType;
    }

    List<ViewType> viewTypes = new ArrayList<ViewType>();

    void addView(final ViewType viewType) {
        viewTypes.add(viewType);
    }

    List<ViewType> getViews() {
        return viewTypes;
    }

    int size() {
        return viewTypes.size();
    }

}