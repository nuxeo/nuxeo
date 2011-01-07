package org.nuxeo.ecm.core.storage.sql;

public class DatabaseH2WithMVCCEnabled extends DatabaseH2 {

    @Override
    protected void setProperties() {
        super.setProperties();
        url += ";MVCC=TRUE";
    }

}
