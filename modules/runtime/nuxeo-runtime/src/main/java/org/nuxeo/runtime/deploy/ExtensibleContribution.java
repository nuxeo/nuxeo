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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class ExtensibleContribution extends Contribution {

    protected ExtensibleContribution baseContribution;

    protected String baseContributionId;

    /**
     * Copy this contribution data over the given one.
     * <p>
     * Warn that the copy must be done deeply - you should clone every element in any collection you have. This is to
     * avoid merging data you copy into the base contribution and breaking subsequent merging operations.
     * <p>
     * The baseContributionId and contributionId fields should not be copied since their are copied by the base classes
     * implementation.
     */
    protected abstract void copyOver(ExtensibleContribution contrib);

    public String getBaseContributionId() {
        return baseContributionId;
    }

    public void setBaseContribution(ExtensibleContribution baseContribution) {
        this.baseContribution = baseContribution;
    }

    public void setBaseContributionId(String baseContributionId) {
        this.baseContributionId = baseContributionId;
    }

    @Override
    public void resolve(ContributionManager mgr) {
        if (baseContributionId != null) {
            baseContribution = (ExtensibleContribution) mgr.getResolved(baseContributionId);
        }
    }

    @Override
    public void unresolve(ContributionManager mgr) {
        baseContribution = null;
    }

    public ExtensibleContribution getBaseContribution() {
        return baseContribution;
    }

    public ExtensibleContribution getRootContribution() {
        return baseContribution == null ? this : baseContribution.getRootContribution();
    }

    public boolean isRootContribution() {
        return baseContribution == null;
    }

    protected ExtensibleContribution getMergedContribution() {
        if (baseContribution == null) {
            return clone();
        }
        ExtensibleContribution mc = baseContribution.getMergedContribution();
        copyOver(mc);
        mc.contributionId = contributionId;
        mc.baseContributionId = baseContributionId;
        return mc;
    }

    @Override
    public void install(ManagedComponent comp) {
        install(comp, getMergedContribution());
    }

    @Override
    public void uninstall(ManagedComponent comp) {
        uninstall(comp, getMergedContribution());
    }

    /**
     * perform a deep clone to void sharing collection elements between clones
     */
    @Override
    public ExtensibleContribution clone() {
        ExtensibleContribution clone;
        try {
            clone = getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate the contribution class. "
                    + "Contribution classes must have a trivial constructor", e);
        }
        copyOver(clone);
        clone.contributionId = contributionId;
        clone.baseContributionId = baseContributionId;
        return clone;
    }

}
