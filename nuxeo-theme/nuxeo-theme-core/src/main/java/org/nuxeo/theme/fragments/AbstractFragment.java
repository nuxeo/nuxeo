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

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.AbstractElement;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.perspectives.PerspectiveType;

public abstract class AbstractFragment extends AbstractElement implements
        Fragment {

    public abstract Model getModel() throws ModelException;

    private FragmentType fragmentType;

    public void setFragmentType(FragmentType fragmentType) {
        this.fragmentType = fragmentType;
    }

    public FragmentType getFragmentType() {
        return fragmentType;
    }

    public boolean isVisibleInPerspective(PerspectiveType perspective) {
        return Manager.getPerspectiveManager().isVisibleInPerspective(this,
                perspective);
    }

    public void setVisibleInPerspective(PerspectiveType perspective) {
        PerspectiveManager.setVisibleInPerspective(this, perspective);
    }

    public void setAlwaysVisible() {
        Manager.getPerspectiveManager().setAlwaysVisible(this);
    }

    public List<PerspectiveType> getVisibilityPerspectives() {
        return Manager.getPerspectiveManager().getPerspectivesFor(this);
    }

    public boolean isDynamic() {
        return fragmentType.isDynamic();
    }

}
