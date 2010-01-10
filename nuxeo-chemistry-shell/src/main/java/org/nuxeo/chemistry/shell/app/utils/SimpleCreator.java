package org.nuxeo.chemistry.shell.app.utils;

import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;

public class SimpleCreator {

    protected final Folder folder;

    public SimpleCreator(Folder folder) {
        this.folder = folder;
    }

    public void createFolder(String name) throws Exception {
        Folder newFolder = folder.newFolder("Workspace");
        newFolder.setName(name);
        newFolder.setValue("dc:title", name);
        newFolder.save();
    }

    public void createFile(String name) throws Exception {
        Document newDoc = folder.newDocument("File");
        newDoc.setName(name);
        newDoc.setValue("dc:title", name);
        newDoc.save();
    }

}
