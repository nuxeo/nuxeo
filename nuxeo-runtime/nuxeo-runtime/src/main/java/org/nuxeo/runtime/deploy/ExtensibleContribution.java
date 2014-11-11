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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ExtensibleContribution extends Contribution {

    private static final Log log = LogFactory.getLog(ExtensibleContribution.class);

    protected ExtensibleContribution baseContribution;
    protected String baseContributionId;

    /**
     * Copy this contribution data over the given one.
     * <p>
     * Warn that the copy must be done deeply - you should clone every element
     * in any collection you have.
     * This is to avoid merging data you copy into the base contribution
     * and breaking subsequent merging operations.
     * <p>
     * The baseContributionId and contributionId fields should not be copied
     * since their are copied by the base classes implementation.
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
            baseContribution = (ExtensibleContribution)mgr.getResolved(baseContributionId);
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

    protected ExtensibleContribution getMergedContribution() throws Exception {
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
    public void install(ManagedComponent comp) throws Exception {
        install(comp, getMergedContribution());
    }

    @Override
    public void uninstall(ManagedComponent comp) throws Exception {
        uninstall(comp, getMergedContribution());
    }

    /**
     * perform a deep clone to void sharing collection elements between clones
     */
    @Override
    public ExtensibleContribution clone() throws CloneNotSupportedException {
        try {
            ExtensibleContribution clone = getClass().newInstance();
            copyOver(clone);
            clone.contributionId = contributionId;
            clone.baseContributionId = baseContributionId;
            return clone;
        } catch (Exception e) {
            log.error(e);
            throw new CloneNotSupportedException(
                    "Failed to instantiate the contribution class. Contribution classes must have a trivial constructor");
        }
    }

}
