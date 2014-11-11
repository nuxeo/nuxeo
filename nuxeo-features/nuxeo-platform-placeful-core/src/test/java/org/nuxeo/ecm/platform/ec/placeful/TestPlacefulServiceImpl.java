package org.nuxeo.ecm.platform.ec.placeful;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;

/**
 * Test the event conf service.
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestPlacefulServiceImpl extends RepositoryOSGITestCase {

	private static final Log log = LogFactory.getLog(PlacefulServiceImpl.class);
	
    private PlacefulServiceImpl placefulServiceImpl;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.placeful.core");

        deployContrib("org.nuxeo.ecm.platform.placeful.core",
                "OSGI-INF/nxplacefulservice-configs-contrib.xml");

        PlacefulServiceImpl.persistenceProvider.setHibernateConfiguration(new TestHibernateConfiguration());

        placefulServiceImpl = (PlacefulServiceImpl) runtime.getComponent(PlacefulService.ID);

        assertNotNull(placefulServiceImpl);

        openRepository();

    }

    protected DocumentModel doCreateDocument() throws ClientException {
        DocumentModel rootDocument = coreSession.getRootDocument();
        DocumentModel model = coreSession.createDocumentModel(
                rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        DocumentModel source = coreSession.createDocument(model);
        coreSession.save();
        waitForEventsDispatched();
        return source;
    }

    public void testAnnotations() throws DocumentException, ClientException, ClassNotFoundException {
        SubscriptionConfig config = new SubscriptionConfig(); 
        //DocumentModel source = doCreateDocument();

        config.setEvent("deleted");
        config.setId("000123-023405-045697");
        placefulServiceImpl.setAnnotation(config);
        
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("event", "deleted");
        paramMap.put("id", "000123-023405-045697");
        
        List annotations = placefulServiceImpl.getAnnotationListByParamMap(paramMap, "SubscriptionConfig");
        log.info("Nombre d'annotations en bases : "+ annotations.size());
        assertTrue(annotations.size()>0);
        
        Annotation annotation = placefulServiceImpl.getAnnotation("000123-023405-045697", "SubscriptionConfig");
        assertTrue(annotation != null);
    }
}
