/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.spi.dws;

import java.util.List;

import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dummy.DummyWSSListItem;

public class DWSMetaDataImpl implements DWSMetaData {

    protected User currentUser;

    protected List<User> users;

    protected List<WSSListItem> documents;

    protected List<Link> links;

    protected List<Task> tasks;

    protected Site site;

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public void setDocuments(List<WSSListItem> documents) {
        this.documents = documents;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public List<WSSListItem> getDocuments() {
        return documents;
    }

    public List<Link> getLinks() {
        return links;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<User> getUsers() {
        return users;
    }

    public Site getSite() {
        if (site == null) {
            site = new SiteImpl("MySite");
            ((SiteImpl) site).setItem(new DummyWSSListItem("MySite", "", null));
        }
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }
}
