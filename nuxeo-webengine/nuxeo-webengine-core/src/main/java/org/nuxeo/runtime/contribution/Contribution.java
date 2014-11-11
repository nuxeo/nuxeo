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

package org.nuxeo.runtime.contribution;

import java.util.Set;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Contribution<K, T> extends Iterable<T> {

    ContributionRegistry<K, T> getRegistry();

    K getId();

    Set<Contribution<K, T>> getDependencies();

    Set<Contribution<K, T>> getDependents();

    Set<Contribution<K, T>> getUnresolvedDependencies();

    void addFragment(T fragment, K... superKeys);

    boolean removeFragment(T fragment);

    T getValue();

    int size();

    boolean isEmpty();

    T getFragment(int index);

    boolean isResolved();

    boolean isPhantom();

    boolean isRegistered();

    void unregister();

    void resolve();

    void unresolve();

}
