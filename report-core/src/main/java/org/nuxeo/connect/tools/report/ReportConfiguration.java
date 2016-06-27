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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

import org.nuxeo.connect.tools.report.ReportConfiguration.Contribution;

/**
 *
 *
 * @since 8.3
 */
public class ReportConfiguration extends SimpleContributionRegistry<Contribution> implements Iterable<Contribution> {

    @XObject("report")
    public static class Contribution {

        @XNode("@name")
        String name = "noop";

        @XNode("@enabled")
        boolean enabled = true;

        @XNode("@oftype")
        public void oftype(Class<? extends ReportWriter> typeof) throws ReflectiveOperationException {
            writer = typeof.newInstance();
        }

        ReportWriter writer;

    }

    @Override
    public String getContributionId(Contribution contrib) {
        return contrib.name;
    }

    @Override
    public boolean isSupportingMerge() {
        return true;
    }

    @Override
    public void merge(Contribution src, Contribution dst) {
        dst.writer = src.writer;
        dst.enabled = src.enabled;
    }

    @Override
    public Contribution clone(Contribution orig) {
        Contribution clone = new Contribution();
        clone.name = orig.name;
        clone.writer = orig.writer;
        clone.enabled = orig.enabled;
        return clone;
    }

    @Override
    public Iterator<Contribution> iterator() {
        return filter(Collections.emptySet()).iterator();
    }

    Iterable<Contribution> filter(Set<String> names) {
        return new Iterable<Contribution>() {

            @Override
            public Iterator<Contribution> iterator() {
                return new Iterator<Contribution>() {
                    final Iterator<Contribution> iterator = currentContribs.values().iterator();

                    @Override
                    public boolean hasNext() {
                        return fetch();
                    }

                    Contribution next;

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
                    public Contribution next() {
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
