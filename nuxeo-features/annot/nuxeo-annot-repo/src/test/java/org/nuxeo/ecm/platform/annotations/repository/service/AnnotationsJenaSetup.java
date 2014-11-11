package org.nuxeo.ecm.platform.annotations.repository.service;

import java.util.Properties;

import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;

class AnnotationsJenaSetup implements RuntimeServiceListener {
    @Override
    public void handleEvent(RuntimeServiceEvent event) {
        if (event.id == RuntimeServiceEvent.RUNTIME_ABOUT_TO_START) {
            Framework.removeListener(this);
        }
        final Properties properties = Framework.getProperties();
        properties.setProperty("org.nuxeo.ecm.sql.jena.databaseType",
                "HSQL");
        properties.setProperty(
                "org.nuxeo.ecm.sql.jena.databaseTransactionEnabled",
                "false");
    }
}