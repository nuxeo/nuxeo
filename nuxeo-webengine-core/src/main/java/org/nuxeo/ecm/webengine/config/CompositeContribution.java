/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.config;

import java.util.ArrayList;
import java.util.List;




/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class CompositeContribution extends ExtensibleContribution {

    protected List<CompositeContribution> fragments = new ArrayList<CompositeContribution>();

    private boolean isEnabled;

    @Override
    public void resolve(ContributionManager mgr) {
        super.resolve(mgr);
        if (base instanceof CompositeContribution) {
            ((CompositeContribution)base).addFragment(this);
        }
    }

    @Override
    public void unresolve(ContributionManager mgr) {
        if (base instanceof CompositeContribution) {
            ((CompositeContribution)base).removeFragment(this);
        }
        super.unresolve(mgr);
    }

    /**
     * @return the isEnabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * @param isEnabled the isEnabled to set.
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void addFragment(CompositeContribution fragment) {
        fragment.setEnabled(true);
        int index = fragments.indexOf(fragment);
        if (index > -1) {
            fragments.set(index, fragment);
        } else {
            fragments.add(fragment);
        }
    }


    public void removeFragment(CompositeContribution fragment) {
        int index = fragments.indexOf(fragment);
        if (index > -1) { // do not physically remove fragments since they can be reloaded
            fragments.get(index).setEnabled(false);
        }
    }

    public List<CompositeContribution> getFragments() {
        return fragments;
    }

    @Override
    protected ExtensibleContribution getMergedContribution() throws Exception {
        ExtensibleContribution mc = super.getMergedContribution();
        for (CompositeContribution fragment : fragments) {
            if (fragment.isEnabled()) {
                fragment.copyOver(mc);
            }
        }
        return mc;
    }

}
