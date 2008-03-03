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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemEntry;

/**
 * Work item entry entity bean.
 * <p>
 * WorkItemEntry implementation with JPA storage.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Entity
@Table(name = "WORK_ITEM_ENTRIES")
public class WorkItemEntryImpl implements WorkItemEntry {

    private static final long serialVersionUID = 1L;

    protected int entryId;

    protected String wiName;

    protected int wiOrder;

    protected String wiDirective;

    protected String wiParticipant;

    protected Date wiDueDate;

    protected String wiComment;

    protected WorkItemsListEntryImpl workItemsListEntry;


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ENTRY_ID", nullable = false, columnDefinition = "integer")
    public int getEntryId() {
        return entryId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    @Column(name = "WI_NAME")
    public String getWiName() {
        return wiName;
    }

    public void setWiName(String wiName) {
        this.wiName = wiName;
    }

    @Column(name = "WI_ORDER", columnDefinition = "integer")
    public int getWiOrder() {
        return wiOrder;
    }

    public void setWiOrder(int wiOrder) {
        this.wiOrder = wiOrder;
    }

    @Column(name = "WI_DIRECTIVE")
    public String getWiDirective() {
        return wiDirective;
    }

    public void setWiDirective(String wiDirective) {
        this.wiDirective = wiDirective;
    }

    @Column(name = "WI_PARTICIPANT")
    public String getWiParticipant() {
        return wiParticipant;
    }

    public void setWiParticipant(String wiParticipant) {
        this.wiParticipant = wiParticipant;
    }

    @ManyToOne
    @JoinColumn(name = "WORK_ITEMS_LIST_ENTRY_ID", nullable = true)
    public WorkItemsListEntryImpl getWorkItemsListEntry() {
        return workItemsListEntry;
    }

    public void setWorkItemsListEntry(WorkItemsListEntryImpl workItemsListEntry) {
        this.workItemsListEntry = workItemsListEntry;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "WI_DUEDATE")
    public Date getWiDueDate() {
        return wiDueDate;
    }

    public void setWiDueDate(Date wiDueDate) {
        this.wiDueDate = wiDueDate;
    }

    @Column(name = "WI_COMMENT")
    public String getWiComment() {
        return wiComment;
    }

    public void setWiComment(String wiComment) {
        this.wiComment = wiComment;
    }

}
