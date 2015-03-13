package org.nuxeo.ecm.platform.ec.notification;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, CoreFeature.class})
public class UserSubscriptionAdapterTest {
    
    @Inject
    private CoreSession session;

    @Test
    public void aDocumentMayHaveAUserSubscriptionAdapter() throws Exception {
        //Given a docuemnt
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "Note");
        doc = session.createDocument(doc);
        
        //I can get a user subscription parameter on it
        UserSubscription  us = doc.getAdapter(UserSubscription.class);
        assertNotNull("It should be able to get a UserSubscription adapter", us);
        
        
        //To get and set its properties
        
        
        
    }
    
}
