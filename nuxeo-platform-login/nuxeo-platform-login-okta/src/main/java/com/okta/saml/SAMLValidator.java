package com.okta.saml;

import org.opensaml.DefaultBootstrap;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.xml.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * Wrapper for the other Okta SAML classes
 * Runs the initialization for libraries used by the other classes
 */
public class SAMLValidator {

    private static final Logger logger = LoggerFactory.getLogger(SAMLValidator.class);

    /**
     * Constructs a SAMLValidator
     * @throws SecurityPolicyException if there is a problem while loading the openSAML library
     */
    public SAMLValidator() throws SecurityPolicyException {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
            throw new SecurityPolicyException("Problem while bootstrapping openSAML library");
        }
    }

    /**
     * Wrapper for Configuration constructor
     *
     * @param config the configuration file content in string format
     * @return Configuration generated from the given configuration
     * @throws SecurityPolicyException if there is a problem while constructing Configuration
     */
    public Configuration getConfiguration(String config) throws SecurityPolicyException {
        try {
            return new Configuration(config);
        } catch (Exception e) {
            logger.error("Failed to create new Configuration instance", e);
            throw new SecurityPolicyException("Problem parsing the configuration.");
        }
    }

    /**
     * Wrapper for Configuration constructor
     *
     * @param @path the path to configuration file
     * @return Configuration generated from the given configuration
     * @throws SecurityPolicyException if there is a problem while constructing Configuration
     */
    public Configuration getConfigurationFrom(String path) throws SecurityPolicyException, IOException {
        FileInputStream stream = null;
        try {
            stream= new FileInputStream(new File(path));
            FileChannel channel = stream.getChannel();
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            String config = Charset.forName("UTF-8").decode(buffer).toString();
            return getConfiguration(config);
        } catch (FileNotFoundException e) {
            logger.error("File not found. Current path: " + new File(".").getAbsolutePath());
            throw e;
        } finally {
            if (stream != null) stream.close();
        }
    }

    /**
     * Wrapper for SAMLRequest constructor
     *
     * @param application Application that includes the URL where the SAMLRequest should be sent to and the issuer
     * @return the SAMLRequest created from the given application
     */
    public SAMLRequest getSAMLRequest(Application application) {
        return new SAMLRequest(application);
    }

    /**
     * Wrapper for SAMLResponse constructor
     *
     * @param responseString The SAMLResponse sent by an IdP. The responseString must NOT be Base64 encoded.
     * @param configuration Configuration that includes the IdP's public certificate necessary to
     *                      verify the responseString's signature.
     * @throws SecurityPolicyException if there is a problem while parsing or validating the response
     */
    public SAMLResponse getSAMLResponse(String responseString, Configuration configuration) throws SecurityPolicyException {
        return new SAMLResponse(responseString, configuration);
    }
}
