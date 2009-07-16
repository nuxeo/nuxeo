package org.nuxeo.ecm.core.persistence;




import java.net.URL;

import javax.persistence.EntityManager;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for persistence
 *
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public abstract class PersistenceTestCase extends TestCase {

    protected static final Log log = LogFactory.getLog(PersistenceTestCase.class);

    protected PersistenceProvider persistenceProvider;

    protected EntityManager entityManager;


    protected  void handleBeforeSetup(HibernateConfiguration config) {
        
    }
    protected void handleAfterSetup(EntityManager entityManager) {
        
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        URL resource = getClass().getResource("/hibernate-tests.xml");
        HibernateConfiguration config = HibernateConfiguration.load(resource);
        persistenceProvider = new PersistenceProvider(config); 
        handleBeforeSetup(config);
        persistenceProvider.openPersistenceUnit();
        entityManager = persistenceProvider.acquireEntityManagerWithActiveTransaction();
        handleAfterSetup(entityManager);
    }

    @Override
    public void tearDown() {
        persistenceProvider.releaseEntityManagerWithRollback(entityManager);
    }

  
}
