/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.TaskInfo;

/**
 * Wraps the list of {@link GraphNode.TaskInfo} on a {@link GraphNode} to expose in a pretty way information to MVEL
 * scripts.
 *
 * @since 5.7.3
 */
public class TasksInfoWrapper implements List<GraphNode.TaskInfo>, Serializable {

    private static final long serialVersionUID = 1L;

    protected List<GraphNode.TaskInfo> tasks;

    public TasksInfoWrapper() {
        tasks = new ArrayList<>();
    }

    public TasksInfoWrapper(List<GraphNode.TaskInfo> tasks) {
        this.tasks = tasks;
    }

    public int getNumberEndedWithStatus(String status) {
        int noEndedWithStatus = 0;
        for (GraphNode.TaskInfo taskInfo : tasks) {
            if (taskInfo.getStatus() != null && status.equals(taskInfo.getStatus())) {
                noEndedWithStatus++;
            }
        }
        return noEndedWithStatus;
    }

    @Override
    public int size() {
        return tasks.size();
    }

    @Override
    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return tasks.contains(o);
    }

    @Override
    public Iterator<TaskInfo> iterator() {
        return tasks.iterator();
    }

    @Override
    public Object[] toArray() {
        return tasks.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return tasks.toArray(a);
    }

    @Override
    public boolean add(TaskInfo e) {
        return tasks.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return tasks.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return tasks.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends TaskInfo> c) {
        return tasks.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends TaskInfo> c) {
        return tasks.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return tasks.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return tasks.retainAll(c);
    }

    @Override
    public void clear() {
        tasks.clear();

    }

    @Override
    public TaskInfo get(int index) {
        return tasks.get(index);
    }

    @Override
    public TaskInfo set(int index, TaskInfo element) {
        return tasks.set(index, element);
    }

    @Override
    public void add(int index, TaskInfo element) {
        tasks.add(index, element);

    }

    @Override
    public TaskInfo remove(int index) {
        return tasks.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return tasks.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return tasks.lastIndexOf(o);
    }

    @Override
    public ListIterator<TaskInfo> listIterator() {
        return tasks.listIterator();
    }

    @Override
    public ListIterator<TaskInfo> listIterator(int index) {
        return tasks.listIterator(index);
    }

    @Override
    public List<TaskInfo> subList(int fromIndex, int toIndex) {
        return tasks.subList(fromIndex, toIndex);
    }
}
