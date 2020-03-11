/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    protected final List<CompositeContribution> contributionFragments = new ArrayList<>();

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
    protected ExtensibleContribution getMergedContribution() {
        CompositeContribution root = getRootComposite();
        ExtensibleContribution mc = root.baseContribution != null ? root.baseContribution.getMergedContribution()
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
