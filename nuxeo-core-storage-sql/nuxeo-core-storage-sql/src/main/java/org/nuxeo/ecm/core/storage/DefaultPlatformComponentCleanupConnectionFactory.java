package org.nuxeo.ecm.core.storage;

//this is required so we don't introduce a dependency between the SQL
//and the RA level... the RA depends on SQL but not the reverse
public interface DefaultPlatformComponentCleanupConnectionFactory {
    DefaultPlatformComponentCleanupManagedConnectionFactory getManagedConnectionFactory();
}
