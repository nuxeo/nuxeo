package org.nuxeo.okta.tests;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.okta.saml.Application;
import com.okta.saml.Configuration;
import com.okta.saml.SAMLValidator;

public class TestInitOkta {

    
    @Test
    public void testConfig() throws Exception {
        Application oktaApp;
        SAMLValidator validator;
        Configuration configuration;
        
        validator = new SAMLValidator();
        String configFileName = "/sample-okta-config.xml";        
        InputStream stream = this.getClass().getResourceAsStream(configFileName);
        String oktaXmlConfig = IOUtils.toString(stream, "UTF-8");
        // Load configuration from the xml for the template app
        configuration = validator.getConfiguration(oktaXmlConfig);
        oktaApp = configuration.getDefaultApplication();        

    }
}
