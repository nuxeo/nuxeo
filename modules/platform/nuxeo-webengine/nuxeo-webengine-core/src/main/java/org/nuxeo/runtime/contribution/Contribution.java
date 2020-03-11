/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
