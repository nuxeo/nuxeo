package org.nuxeo.sitefactory.api;

import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface ContentEntry {

    DocumentModel getDocument();
    
    String getTitle();
    
    String getAbstract();
    
    String getAuthor();
    
    List<String> getContributors();
    
    Calendar getCreationDate();
    
    Calendar getModificationDate();
    
    Calendar getPublicationDate();
    
    List<String> getRenditionNames();
    
    Blob getRenderedContent() throws Exception ;
    
    Blob getRenderedContent(String renditionName) throws Exception ;
    
    List<Blob> getAttachements();
    
}
