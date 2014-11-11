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
 * This is a contribution registry that is managing contribution fragments and merge them as needed.
 * The implementation will be notified through {@link #contributionUpdated(String, Object)} each time
 * you need to store or remove a contribution. Note that contribution objects that are registered by
 * your implementation <b>must</b> not be modified. You can see them as immutable objects - otherwise your local changes
 * will be lost at the next update event.
 * <p>
 * To use it you should extends this abstract implementation and implement the abstract methods.
 * <p>
 * The implementation registry doesn't need to be thread safe since it will be called from synchronized methods.
 * <p>
 * Also, the contribution object
 * <p>
 * A simple implementation is:
 * <pre>
 *   public class MyRegistry extends ContributionFragmentRegistry<MyContribution> {
 *       public Map<String, MyContribution> registry = new HAshMap<String,MyContribution>();
 *       public String getContributionId(MyContribution contrib) {
 *          return contrib.getId();
 *       }
 *       public void contributionUpdated(String id, MyContribution contrib, MyContribution origContrib) {
 *           registry.put(id, contrib);
 *       }
 *       public void contributionRemoved(String id, MyContribution origContrib) {
 *           registry.remove(id);
 *       }
 *       public MyContribution clone(MyContribution contrib) {
 *          MyContribution clone = new MyContribution(contrib.getId());
 *          clone.setSomeProperty(contrib.getSomeProperty());
 *          ...
 *          return clone;
 *       }
 *       public void merge(MyContribution src, MyContribution dst) {
 *          dst.setSomeProperty(src.getSomeProperty());
 *          ...
 *       }
 *   }
 * </pre>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ContributionFragmentRegistry<T> {

    protected Map<String, FragmentList<T>> contribs = new HashMap<String, FragmentList<T>>();

    /**
     * Get the contribution ID given the contribution object
     * @param contrib
     * @return
     */
    public abstract String getContributionId(T contrib);

    /**
     * Add or update a contribution.
     * <p>
     * If the contribution doesn't yet exists then it will be added, otherwise the value will be updated.
     * If the given value is null the existing contribution must be removed
     * <p>
     * The second parameter is the actual contribution value which should be stored (updated).
     * This usually represent a clone of the original contribution or a merge of multiple contribution fragments.
     * Modifications on this object at application level will be lost on next {@link #contributionUpdated(String, Object, Object)}
     * call on the same object id.
     * The last parameter is the original contribution object which was either cloned or merged.
     * You should never modify this object at application level. Because it will be used each time a subsequent
     * merge is done.
     * Also you should not store this object. Usually you will never need to touch this object.
     *
     * @param id - the id of the contribution that need to be updated
     * @param contrib the updated contribution object that
     * @param origContrib - the original contribution fragment that triggered the update.
     */
    public abstract void contributionUpdated(String id, T contrib, T origContrib);

    /**
     * All the fragments in the contribution was removed. Contribution must be unregistered.
     * <p>
     * The first parameter is the contribution ID that should be remove and the second parameter
     * the original contribution fragment that as unregistered causing the contribution to be removed.
     *
     * @param id
     * @param origContrib
     */
    public abstract void contributionRemoved(String id, T origContrib);

    /**
     * CLone the given contribution object
     * @param object
     * @return
     */
    public abstract T clone(T object);

    /**
     * Merge 'src' into 'dst'. When merging only the 'dst' object is modified.
     * @param src the object to copy over the 'dst' object
     * @param dst this object is modified
     */
    public abstract void merge(T src, T dst);


    /**
     * Add a new contribution. This will start install the new contribution and will notify the implementation
     * about the value to add. (the final value to add may not be the same object as the one added -
     * but a merge between multiple contributions)
     * @param contrib
     */
    public synchronized void addContribution(T contrib) {
        String id = getContributionId(contrib);
        FragmentList<T> head = addFragment(id, contrib);
        contributionUpdated(id, head.merge(this), contrib);
    }

    /**
     * Remove a contribution. This will uninstall the contribution and notify the implementation about the new value
     * it should store (after re-merging contribution fragments).
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
     * avoid passing by the implementation registry.
     * Note that this operation will invoke a merge of existing fragments if needed.
     * @param id
     * @return
     */
    public synchronized T getContribution(String id) {
        FragmentList<T> head = contribs.get(id);
        return head != null ? head.merge(this) : null;
    }

    /**
     * Get an array of all contribution fragments
     * @return
     */
    @SuppressWarnings("unchecked")
    public synchronized FragmentList<T>[] getFragments() {
        return contribs.values().toArray(new FragmentList[contribs.size()]);
    }

    private FragmentList<T> addFragment(String id, T contrib) {
        FragmentList<T> head = contribs.get(id);
        if (head == null) {
            // no merge needed
            head = new FragmentList<T>();
            this.contribs.put(id, head);
        }
        head.add(contrib);
        return head;
    }

    private FragmentList<T> removeFragment(String id, T contrib) {
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
           super (null);
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
            mergedValue = reg.clone(p.object);
            p = p.next;
            while (p != this) {
                reg.merge(p.object, mergedValue);
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
