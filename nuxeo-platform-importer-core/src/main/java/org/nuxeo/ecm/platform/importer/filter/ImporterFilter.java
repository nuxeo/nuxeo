package org.nuxeo.ecm.platform.importer.filter;

public interface ImporterFilter {

    void handleBeforeImport();

    void handleAfterImport(Exception e);
}
