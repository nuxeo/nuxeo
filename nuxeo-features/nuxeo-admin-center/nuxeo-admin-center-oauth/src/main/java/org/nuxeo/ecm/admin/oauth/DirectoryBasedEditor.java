package org.nuxeo.ecm.admin.oauth;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public abstract class DirectoryBasedEditor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected DocumentModelList entries;

    protected DocumentModel editableEntry;

    protected DocumentModel creationEntry;

    protected abstract String getDirectoryName();

    protected abstract String getSchemaName();

    protected boolean showAddForm = false;

    public boolean getShowAddForm() {
        return showAddForm;
    }

    public void toggleShowAddForm() {
        showAddForm = !showAddForm;
    }

    public DocumentModel getCreationEntry() throws Exception {
        if (creationEntry == null) {
            creationEntry = BaseSession.createEntryModel(null, getSchemaName(),
                    null, null);
        }
        return creationEntry;
    }

    public void refresh() {
        entries = null;
    }

    public void createEntry() throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(getDirectoryName());
        try {
            session.createEntry(creationEntry);
            creationEntry = null;
            showAddForm = false;
            entries = null;
        } finally {
            session.close();
        }
    }

    public void resetCreateEntry() {
        creationEntry = null;
        showAddForm = false;
    }

    public void resetEditEntry() {
        editableEntry = null;
        showAddForm = false;
    }

    public DocumentModel getEditableEntry() {
        return editableEntry;
    }

    protected Map<String, Serializable> getQueryFilter() {
        return Collections.emptyMap();
    }

    protected Set<String> getOrderSet() {
        return Collections.emptySet();
    }

    public DocumentModelList getEntries() throws Exception {
        if (entries == null) {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            Session session = ds.open(getDirectoryName());
            try {
                Map<String, Serializable> emptyMap = getQueryFilter();
                Set<String> emptySet = getOrderSet();

                entries = session.query(emptyMap, emptySet, null, true);
            } finally {
                session.close();
            }
        }
        return entries;
    }

    public void editEntry(String entryId) throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(getDirectoryName());
        try {
            editableEntry = session.getEntry(entryId);
        } finally {
            session.close();
        }
    }

    public void saveEntry() throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(getDirectoryName());
        try {
            session.updateEntry(editableEntry);
            editableEntry = null;
            entries = null;
        } finally {
            session.close();
        }
    }

    public void deleteEntry(String entryId) throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(getDirectoryName());
        try {
            session.deleteEntry(entryId);
            if (editableEntry != null && editableEntry.getId().equals(entryId)) {
                editableEntry = null;
            }
            entries = null;
        } finally {
            session.close();
        }
    }

}
