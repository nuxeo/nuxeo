package org.nuxeo.ecm.core.persistence;

import javax.persistence.EntityManager;

public class PersistenceProviderFriend {
    
    private PersistenceProviderFriend() {
        ;
    }

    public static EntityManager acquireEntityManager(PersistenceProvider provider) {
        return provider.doAcquireEntityManager();
    }
}
