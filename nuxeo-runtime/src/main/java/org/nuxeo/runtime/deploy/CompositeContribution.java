/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.deploy;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class CompositeContribution extends ExtensibleContribution {

    protected final List<CompositeContribution> contributionFragments = new ArrayList<CompositeContribution>();

    private boolean isContributionEnabled;

    @Override
    public void resolve(ContributionManager mgr) {
        super.resolve(mgr);
        if (baseContribution instanceof CompositeContribution) {
            ((CompositeContribution) baseContribution).addContributionFragment(this);
        }
    }

    @Override
    public void unresolve(ContributionManager mgr) {
        if (baseContribution instanceof CompositeContribution) {
            ((CompositeContribution) baseContribution).removeContributionFragment(this);
        }
        super.unresolve(mgr);
    }

    public boolean isContributionEnabled() {
        return isContributionEnabled;
    }

    private void setContributionEnabled(boolean isEnabled) {
        isContributionEnabled = isEnabled;
    }

    private void addContributionFragment(CompositeContribution fragment) {
        fragment.setContributionEnabled(true);
        int index = contributionFragments.indexOf(fragment);
        if (index > -1) {
            contributionFragments.set(index, fragment);
        } else {
            contributionFragments.add(fragment);
        }
    }

    private void removeContributionFragment(CompositeContribution fragment) {
        int index = contributionFragments.indexOf(fragment);
        if (index > -1) { // do not physically remove fragments since they can be reloaded
            contributionFragments.get(index).setContributionEnabled(false);
        }
    }

    public List<CompositeContribution> getContributionFragments() {
        return contributionFragments;
    }

    private CompositeContribution getRootComposite() {
        if (baseContribution instanceof CompositeContribution) {
            return ((CompositeContribution) baseContribution).getRootComposite();
        }
        return this;
    }

    @Override
    protected ExtensibleContribution getMergedContribution() throws Exception {
        CompositeContribution root = getRootComposite();
        ExtensibleContribution mc = root.baseContribution != null
                ? root.baseContribution.getMergedContribution()
                : root.clone();
        for (CompositeContribution fragment : root.contributionFragments) {
            if (fragment.isContributionEnabled()) {
                copyFragmentsOver(mc);
            }
        }
        mc.contributionId = root.contributionId;
        mc.baseContributionId = root.baseContributionId;
        return mc;
    }

    private void copyFragmentsOver(ExtensibleContribution mc) {
        copyOver(mc);
        for (CompositeContribution fragment : contributionFragments) {
            if (fragment.isContributionEnabled()) {
                fragment.copyFragmentsOver(mc);
            }
        }
    }

    @Override
    public String toString() {
        if (baseContributionId == null) {
            return contributionId;
        }
        return contributionId + "@" + baseContributionId;
    }

}
