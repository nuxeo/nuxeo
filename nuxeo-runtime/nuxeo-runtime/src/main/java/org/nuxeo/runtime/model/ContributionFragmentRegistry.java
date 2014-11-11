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
 */
package org.nuxeo.runtime.model;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a contribution registry that is managing contribution fragments and
 * merge them as needed. The implementation will be notified through
 * {@link #contributionUpdated(String, Object)} each time you need to store or
 * remove a contribution. Note that contribution objects that are registered by
 * your implementation <b>must</b> not be modified. You can see them as
 * immutable objects - otherwise your local changes will be lost at the next
 * update event.
 * <p>
 * To use it you should extends this abstract implementation and implement the
 * abstract methods.
 * <p>
 * The implementation registry doesn't need to be thread safe since it will be
 * called from synchronized methods.
 * <p>
 * Also, the contribution object
 * <p>
 * A simple implementation is:
 *
 * <pre>
 * public class MyRegistry extends ContributionFragmentRegistry&lt;MyContribution&gt; {
 *     public Map&lt;String, MyContribution&gt; registry = new HAshMap&lt;String, MyContribution&gt;();
 *
 *     public String getContributionId(MyContribution contrib) {
 *         return contrib.getId();
 *     }
 *
 *     public void contributionUpdated(String id, MyContribution contrib,
 *             MyContribution origContrib) {
 *         registry.put(id, contrib);
 *     }
 *
 *     public void contributionRemoved(String id, MyContribution origContrib) {
 *         registry.remove(id);
 *     }
 *
 *     public MyContribution clone(MyContribution contrib) {
 *          MyContribution clone = new MyContribution(contrib.getId());
 *          clone.setSomeProperty(contrib.getSomeProperty());
 *          ...
 *          return clone;
 *       }
 *
 *     public void merge(MyContribution src, MyContribution dst) {
 *          dst.setSomeProperty(src.getSomeProperty());
 *          ...
 *       }
 * }
 * </pre>
 *
 * Since 5.5, if the registry does not support merging of resources, you can
 * just override the method {@link #isSupportingMerge()} and return false, so
 * that {@link #merge(Object, Object)} and {@link #clone()} are never called.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @see SimpleContributionRegistry<T>
 */
public abstract class ContributionFragmentRegistry<T> {

    protected Map<String, FragmentList<T>> contribs = new HashMap<String, FragmentList<T>>();

    /**
     * Returns the contribution ID given the contribution object
     *
     * @param contrib
     * @return
     */
    public abstract String getContributionId(T contrib);

    /**
     * Adds or updates a contribution.
     * <p>
     * If the contribution doesn't yet exists then it will be added, otherwise
     * the value will be updated. If the given value is null the existing
     * contribution must be removed.
     * <p>
     * The second parameter is the contribution that should be updated when
     * merging, as well as stored and used. This usually represents a clone of
     * the original contribution or a merge of multiple contribution fragments.
     * Modifications on this object at application level will be lost on next
     * {@link #contributionUpdated(String, Object, Object)} call on the same
     * object id: modifications should be done in the
     * {@link #merge(Object, Object)} method.
     * <p>
     * The last parameter is the new contribution object, unchanged (original)
     * which was neither cloned nor merged. This object should never be
     * modified at application level, because it will be used each time a
     * subsequent merge is done. Also, it never should be stored.
     *
     * @param id - the id of the contribution that needs to be updated
     * @param contrib the updated contribution object that
     * @param newOrigContrib - the new, unchanged (original) contribution
     *            fragment that triggered the update.
     */
    public abstract void contributionUpdated(String id, T contrib,
            T newOrigContrib);

    /**
     * All the fragments in the contribution was removed. Contribution must be
     * unregistered.
     * <p>
     * The first parameter is the contribution ID that should be remove and the
     * second parameter the original contribution fragment that as unregistered
     * causing the contribution to be removed.
     *
     * @param id
     * @param origContrib
     */
    public abstract void contributionRemoved(String id, T origContrib);

    /**
     * CLone the given contribution object
     *
     * @param object
     * @return
     */
    public abstract T clone(T orig);

    /**
     * Merge 'src' into 'dst'. When merging only the 'dst' object is modified.
     *
     * @param src the object to copy over the 'dst' object
     * @param dst this object is modified
     */
    public abstract void merge(T src, T dst);

    /**
     * Returns true if merge is supported.
     * <p>
     * Hook method to be overridden if merge logics behind {@link #clone()} and
     * {@link #merge(Object, Object)} cannot be implemented.
     *
     * @since 5.5
     */
    public boolean isSupportingMerge() {
        return true;
    }

    /**
     * Add a new contribution. This will start install the new contribution and
     * will notify the implementation about the value to add. (the final value
     * to add may not be the same object as the one added - but a merge between
     * multiple contributions)
     *
     * @param contrib
     */
    public synchronized void addContribution(T contrib) {
        String id = getContributionId(contrib);
        FragmentList<T> head = addFragment(id, contrib);
        contributionUpdated(id, head.merge(this), contrib);
    }

    /**
     * Remove a contribution. This will uninstall the contribution and notify
     * the implementation about the new value it should store (after re-merging
     * contribution fragments).
     *
     * @param contrib
     */
    public synchronized void removeContribution(T contrib) {
        String id = getContributionId(contrib);
        FragmentList<T> head = removeFragment(id, contrib);
        if (head != null) {
            T result = head.merge(this);
            if (result != null) {
                contributionUpdated(id, result, contrib);
            } else {
                contributionRemoved(id, contrib);
            }
        }
    }

    /**
     * Get a merged contribution directly from the internal registry - and
     * avoid passing by the implementation registry. Note that this operation
     * will invoke a merge of existing fragments if needed.
     * <p>
     * Since 5.5, this method has made protected as it should not be used by
     * the service retrieving merged resources (otherwise merge will be done
     * again). If you'd really like to call it, add a public method on your
     * registry implementation that will call it.
     *
     * @param id
     * @return
     */
    protected synchronized T getContribution(String id) {
        FragmentList<T> head = contribs.get(id);
        return head != null ? head.merge(this) : null;
    }

    /**
     * Get an array of all contribution fragments
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public synchronized FragmentList<T>[] getFragments() {
        return contribs.values().toArray(new FragmentList[contribs.size()]);
    }

    protected FragmentList<T> addFragment(String id, T contrib) {
        FragmentList<T> head = contribs.get(id);
        if (head == null) {
            // no merge needed
            head = new FragmentList<T>();
            this.contribs.put(id, head);
        }
        head.add(contrib);
        return head;
    }

    protected FragmentList<T> removeFragment(String id, T contrib) {
        FragmentList<T> head = contribs.get(id);
        if (head == null) {
            return null;
        }
        if (head.remove(contrib)) {
            if (head.isEmpty()) {
                contribs.remove(id);
            }
            return head;
        }
        return null;
    }

    public static class FragmentList<T> extends Fragment<T> {

        public FragmentList() {
            super(null);
            prev = this;
            next = this;
        }

        public boolean isEmpty() {
            return next == null;
        }

        public T merge(ContributionFragmentRegistry<T> reg) {
            T mergedValue = object;
            if (mergedValue != null) {
                return mergedValue;
            }
            Fragment<T> p = next;
            if (p == this) {
                return null;
            }
            mergedValue = reg.isSupportingMerge() ? reg.clone(p.object)
                    : p.object;
            p = p.next;
            while (p != this) {
                if (reg.isSupportingMerge()) {
                    reg.merge(p.object, mergedValue);
                } else {
                    mergedValue = p.object;
                }
                p = p.next;
            }
            object = mergedValue;
            return mergedValue;
        }

        public final void add(T contrib) {
            insertBefore(new Fragment<T>(contrib));
            object = null;
        }

        public final void add(Fragment<T> fragment) {
            insertBefore(fragment);
            object = null;
        }

        public boolean remove(T contrib) {
            Fragment<T> p = next;
            while (p != this) {
                // contributions come from the runtime that keeps exact
                // instances, so using equality here makes it possible to
                // remove the exact instance that was contributed by this
                // component (without needing to reference the component name
                // for instance)
                if (p.object == contrib) {
                    p.remove();
                    object = null;
                    return true;
                }
                p = p.next;
            }
            return false;
        }
    }

    public static class Fragment<T> {
        public T object;

        public Fragment<T> next;

        public Fragment<T> prev;

        public Fragment(T object) {
            this.object = object;
        }

        public final void insertBefore(Fragment<T> fragment) {
            fragment.prev = prev;
            fragment.next = this;
            prev.next = fragment;
            prev = fragment;
        }

        public final void insertAfter(Fragment<T> fragment) {
            fragment.prev = this;
            fragment.next = next;
            next.prev = fragment;
            next = fragment;
        }

        public final void remove() {
            prev.next = next;
            next.prev = prev;
            next = prev = null;
        }

        public final boolean hasNext() {
            return next != null;
        }

        public final boolean hasPrev() {
            return prev != null;
        }
    }

}
