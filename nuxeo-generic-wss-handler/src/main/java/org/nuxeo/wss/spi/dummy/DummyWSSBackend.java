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

package org.nuxeo.wss.spi.dummy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.AbstractWSSBackend;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.DWSMetaDataImpl;
import org.nuxeo.wss.spi.dws.Link;
import org.nuxeo.wss.spi.dws.LinkImpl;
import org.nuxeo.wss.spi.dws.Site;
import org.nuxeo.wss.spi.dws.SiteImpl;
import org.nuxeo.wss.spi.dws.Task;
import org.nuxeo.wss.spi.dws.TaskImpl;
import org.nuxeo.wss.spi.dws.User;
import org.nuxeo.wss.spi.dws.UserImpl;

public class DummyWSSBackend extends AbstractWSSBackend implements WSSBackend {

    public List<WSSListItem> listItems(String location) {
        return DummyMemoryTree.instance().listItems(location);
    }

    public WSSListItem getItem(String location) throws WSSException {
        return DummyMemoryTree.instance().getItem(location);
    }

    public void discardChanges() throws WSSException {
        // NOP
    }

    public void saveChanges() throws WSSException {
        // NOP
    }

    public WSSListItem moveItem(String oldLocation, String newLocation)
            throws WSSException {
        return null;
    }

    public void removeItem(String location) {
    }

    public WSSListItem createFolder(String location, String name) {
        return null;
    }

    public WSSListItem createFileItem(String location, String name)
            throws WSSException {
        return null;
    }

    public DWSMetaData getMetaData(String location, WSSRequest request) throws WSSException {

        DWSMetaDataImpl metadata = new DWSMetaDataImpl();

        User currentUser = new UserImpl("1", "toto", "toto", "toto@somewhere.com");
        metadata.setCurrentUser(currentUser);

        List<User> users = new ArrayList<User>();
        users.add(currentUser);
        users.add(new UserImpl("2", "titi", "titi", "titi@somewhere.com"));
        users.add(new UserImpl("3", "tata", "tata", "tata@somewhere.com"));
        users.add(new UserImpl("4", "members"));
        users.add(new UserImpl("5", "mygroup"));
        metadata.setUsers(users);

        int idx = location.lastIndexOf("/");
        String parentPath = location.substring(0,idx);
        List<WSSListItem> documents  = listLeafItems(parentPath);
        metadata.setDocuments(documents);

        Date date = new Date(System.currentTimeMillis());

        List<Link> links = new ArrayList<Link>();
        links.add(new LinkImpl("1","titi",date,date,"", "link comment1", "http://www.google.com"));
        links.add(new LinkImpl("2","tata",date,date,"", "link comment2", "http://www.nuxeo.com"));
        metadata.setLinks(links);

        List<Task> tasks = new ArrayList<Task>();
        TaskImpl task1 =new TaskImpl("1","titi",date,date,"");
        task1.setTaskData("tata", "My Task1", "Hello", date, "high", "In Progress");
        tasks.add(task1);
        TaskImpl task2 =new TaskImpl("2","toto",date,date,"");
        task2.setTaskData("titi", "My Task2", "Hello", date, "medium", "In Progress");
        tasks.add(task2);
        metadata.setTasks(tasks);

        return metadata;
    }

    public Site getSite(String location) {
        SiteImpl site  = new SiteImpl("MySite");
        site.setAccessUrl("");
        return site;
    }

    @Override
    public void begin() throws WSSException {
        // NOP
    }

}
