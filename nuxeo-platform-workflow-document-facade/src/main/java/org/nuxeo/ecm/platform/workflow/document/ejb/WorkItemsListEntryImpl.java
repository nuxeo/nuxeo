/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListEntry;

/**
 * Work items list entity bean.
 * <p>
 * WorkItemsListEntry implementation with JPA storage.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Entity
@Table(name = "WORK_ITEMS_LISTS_ENTRIES")
@NamedQueries({
    @NamedQuery(
        name = "getWorkItemsListEntriesFor",
        query = "FROM WorkItemsListEntryImpl entry WHERE participantName=:participantName AND processName=:processName"),
    @NamedQuery(
        name = "getWorkItemsListEntryByName",
        query = "FROM WorkItemsListEntryImpl entry WHERE participantName=:participantName AND name=:name"),
    @NamedQuery(
        name = "getWorkItemsListEntriesForAll",
        query = "FROM WorkItemsListEntryImpl entry WHERE processName=:processName")
})
public class WorkItemsListEntryImpl implements WorkItemsListEntry {

    private static final long serialVersionUID = 9106698764801448061L;

    protected int entryId;

    protected String name;

    protected String processName;

    protected String participantName;

    protected Set<WorkItemEntryImpl> workItemEntries = new HashSet<WorkItemEntryImpl>();


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ENTRY_ID", nullable = false, columnDefinition = "integer")
    public int getEntryId() {
        return entryId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "workItemsListEntry")
    public Set<WorkItemEntryImpl> getWorkItemEntries() {
        return workItemEntries;
    }

    public void setWorkItemEntries(Set<WorkItemEntryImpl> workItemEntries) {
        this.workItemEntries = workItemEntries;
    }

    @Column(name = "ENTRY_PROCESS_NAME")
    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    @Column(name = "ENTRY_PARTICIPANT")
    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    @Column(name = "ENTRY_NAME")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
