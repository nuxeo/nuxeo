/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.connect.tools.report;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 *
 *
 * @since 8.3
 */
public class ReportConfiguration extends SimpleContributionRegistry<ReportContribution> implements Iterable<ReportContribution> {

    @Override
    public String getContributionId(ReportContribution contrib) {
        return contrib.name;
    }

    @Override
    public boolean isSupportingMerge() {
        return true;
    }

    @Override
    public void merge(ReportContribution src, ReportContribution dst) {
        dst.instance = src.instance;
        dst.enabled = src.enabled;
    }

    @Override
    public ReportContribution clone(ReportContribution orig) {
        ReportContribution clone = new ReportContribution();
        clone.name = orig.name;
        clone.instance = orig.instance;
        clone.enabled = orig.enabled;
        return clone;
    }

    @Override
    public Iterator<ReportContribution> iterator() {
        return filter(Collections.emptySet()).iterator();
    }

    Iterable<ReportContribution> filter(Set<String> names) {
        return new Iterable<ReportContribution>() {

            @Override
            public Iterator<ReportContribution> iterator() {
                return new Iterator<ReportContribution>() {
                    final Iterator<ReportContribution> iterator = currentContribs.values().iterator();

                    @Override
                    public boolean hasNext() {
                        return fetch();
                    }

                    ReportContribution next;

                    boolean fetch() {
                        if (next != null) {
                            return true;
                        }
                        while (iterator.hasNext()) {
                            next = iterator.next();
                            if (!next.enabled) {
                                continue;
                            }
                            if (!names.isEmpty() && !names.contains(next.name)) {
                                continue;
                            }
                            return true;
                        }
                        next = null;
                        return false;
                    }

                    @Override
                    public ReportContribution next() {
                        if (!fetch()) {
                            throw new NoSuchElementException("no more reports");
                        }
                        try {
                            return next;
                        } finally {
                            next = null;
                        }
                    }
                };

            }

        };
    }
}
