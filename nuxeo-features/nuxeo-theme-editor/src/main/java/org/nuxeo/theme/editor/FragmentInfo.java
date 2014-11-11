/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

    private final FragmentType fragmentType;

    public List<ViewType> viewTypes = new ArrayList<ViewType>();

    FragmentInfo(FragmentType fragmentType) {
        this.fragmentType = fragmentType;
    }

    public List<ViewType> getViewTypes() {
        return viewTypes;
    }

    public FragmentType getFragmentType() {
        return fragmentType;
    }

    public void addView(final ViewType viewType) {
        viewTypes.add(viewType);
    }

    public List<ViewType> getViews() {
        return viewTypes;
    }

    public int size() {
        return viewTypes.size();
    }

}
