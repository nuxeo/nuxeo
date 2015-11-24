package com.okta.saml;

import com.okta.saml.util.IPRange;
import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import org.apache.commons.lang.StringUtils;
import org.opensaml.ws.security.SecurityPolicyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.*;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * This class is derived from a xml configuration file;
 * it is used to provide an application required to generate a SAMLRequest,
 * and to validate SAMLResponse.
 */
public class Configuration {

    private Map<String, Application> applications;
    private String defaultEntityID = null;
    private boolean suppressErrors;
    private String loginUri;

    static XPath XPATH;

    private static XPathExpression CONFIGURATION_ROOT_XPATH;
    private static XPathExpression APPLICATIONS_XPATH;
    private static XPathExpression ENTITY_ID_XPATH;
    private static XPathExpression DEFAULT_APP_XPATH;
    private static XPathExpression ADDRESSES_XPATH;
    private static XPathExpression SP_USERNAMES_XPATH;
    private static XPathExpression SP_GROUPS_XPATH;
    private static XPathExpression SUPPRESS_ERRORS_XPATH;
    private static XPathExpression LOGIN_URI_XPATH;

    private IPRange oktaUsersIps;
    private IPRange spUsersIps;
    private List<String> spUsernames;
    private List<String> spGroupnames;

    public static final String SAML_RESPONSE_FORM_NAME = "SAMLResponse";
    public static final String CONFIGURATION_KEY = "okta.config.file";
    public static final String DEFAULT_ENTITY_ID = "okta.config.default_entity_id";
    public static final String REDIR_PARAM = "os_destination";
    public static final String RELAY_STATE_PARAM = "RelayState";

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    static {
        try {
            XPathFactory xPathFactory = new XPathFactoryImpl();
            XPATH = xPathFactory.newXPath();
            XPATH.setNamespaceContext(new MetadataNamespaceContext());

            CONFIGURATION_ROOT_XPATH = XPATH.compile("configuration");
            APPLICATIONS_XPATH = XPATH.compile("applications/application");
            ADDRESSES_XPATH = XPATH.compile("allowedAddresses");
            SP_USERNAMES_XPATH = XPATH.compile("spUsers/username");
            SP_GROUPS_XPATH = XPATH.compile("spGroups/groupname");
            ENTITY_ID_XPATH = XPATH.compile("md:EntityDescriptor/@entityID");
            DEFAULT_APP_XPATH = XPATH.compile("default");
            SUPPRESS_ERRORS_XPATH = XPATH.compile("suppressErrors");
            LOGIN_URI_XPATH = XPATH.compile("loginUri");
        } catch (XPathExpressionException e) {
            logger.error("Failed to create XPathFactory instance", e);
        }
    }

    public Configuration(String configuration) throws XPathExpressionException, CertificateException, UnsupportedEncodingException, SecurityPolicyException {
        InputSource source = new InputSource(new StringReader(configuration));

        Node root = (Node) CONFIGURATION_ROOT_XPATH.evaluate(source, XPathConstants.NODE);
        NodeList applicationNodes = (NodeList) APPLICATIONS_XPATH.evaluate(root, XPathConstants.NODESET);

        defaultEntityID = DEFAULT_APP_XPATH.evaluate(root);
        applications = new HashMap<String, Application>();
        spUsernames = new ArrayList<String>();
        spGroupnames = new ArrayList<String>();

        for (int i = 0; i < applicationNodes.getLength(); i++) {
            Element applicationNode = (Element) applicationNodes.item(i);
            String entityID = ENTITY_ID_XPATH.evaluate(applicationNode);
            Application application = new Application(applicationNode);
            applications.put(entityID, application);
        }

        Element allowedAddresses = (Element) ADDRESSES_XPATH.evaluate(root, XPathConstants.NODE);
        if (allowedAddresses != null) {
            String oktaFrom = (String) XPATH.compile("oktaUsers/ipFrom").evaluate(allowedAddresses, XPathConstants.STRING);
            String oktaTo = (String) XPATH.compile("oktaUsers/ipTo").evaluate(allowedAddresses, XPathConstants.STRING);

            String spFrom = (String) XPATH.compile("spUsers/ipFrom").evaluate(allowedAddresses, XPathConstants.STRING);
            String spTo = (String) XPATH.compile("spUsers/ipTo").evaluate(allowedAddresses, XPathConstants.STRING);

            if (oktaFrom != null) {
                try {
                    oktaUsersIps = new IPRange(oktaFrom, oktaTo);
                } catch (NumberFormatException e) {
                    logger.error("Invalid IP specified for Okta users addresses: " + e.getMessage());
                }
            }

            if (spFrom != null) {
                try {
                    spUsersIps = new IPRange(spFrom, spTo);
                } catch (NumberFormatException e) {
                    logger.error("Invalid IP specified for Service Provider users addresses: " + e.getMessage());
                }
            }
        }

        String suppress = SUPPRESS_ERRORS_XPATH.evaluate(root);
        if (suppress != null) {
            suppress = suppress.trim();
        }
        suppressErrors = (!StringUtils.isBlank(suppress)) ? Boolean.parseBoolean(suppress) : true;

        loginUri = LOGIN_URI_XPATH.evaluate(root);
        if (loginUri != null) {
            loginUri = loginUri.trim();
        }

        NodeList spUnames = (NodeList) SP_USERNAMES_XPATH.evaluate(root, XPathConstants.NODESET);
        if (spUnames != null) {
            for (int i = 0; i < spUnames.getLength(); i++) {
                Element usernameNode = (Element) spUnames.item(i);
                if (!StringUtils.isBlank(usernameNode.getTextContent())) {
                    spUsernames.add(usernameNode.getTextContent().trim());
                }
            }
        }

        NodeList spGnames = (NodeList) SP_GROUPS_XPATH.evaluate(root, XPathConstants.NODESET);
        if (spGnames != null) {
            for (int i = 0; i < spGnames.getLength(); i++) {
                Element groupnameNode = (Element) spGnames.item(i);
                if (!StringUtils.isBlank(groupnameNode.getTextContent())) {
                    spGroupnames.add(groupnameNode.getTextContent().trim());
                }
            }
        }
    }

    /**
     * @return the map of all the applications listed in the config file,
     *          where the entityID of the application's EntityDescriptor is the key
     *          and its representation in Application object is the value
     */
    public Map<String, Application> getApplications() {
        return applications;
    }

    /**
     * @param entityID an identifier for an EntityDescriptor
     * @return an Application whose EntityDescriptor entityID matches the given entityID
     */
    public Application getApplication(String entityID) {
        return applications.get(entityID);
    }

    /**
     * @return the Application whose EntityDescriptor entityID matches the default entityID
     */
    public Application getDefaultApplication() {
        if (StringUtils.isBlank(defaultEntityID)) {
            return null;
        }
        return applications.get(defaultEntityID);
    }

    /**
     * @return the default entityID from the configuration, which in configured under default
     */
    public String getDefaultEntityID() {
        return defaultEntityID;
    }

    /**
     * Is ip allowed for identity provider users (Okta)
     */
    public boolean isIpAllowedForOkta(String ip) {
        try {
            boolean isRejectedForOkta = oktaUsersIps != null && !oktaUsersIps.isAddressInRange(ip);
            boolean isRejectedForSP = spUsersIps != null && !spUsersIps.isAddressInRange(ip);

            //making it more explicit
            if ((isRejectedForOkta && isRejectedForSP) ||
                    (!isRejectedForOkta && !isRejectedForSP) ||
                    (oktaUsersIps == null && isRejectedForSP) ||
                    (spUsersIps == null && oktaUsersIps == null)) {
                return true;
            }

            if ((oktaUsersIps == null && !isRejectedForSP) ||
                    (isRejectedForOkta && !isRejectedForSP)) {
                return false;
            }
        } catch (Exception e) {
            logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
            //something is wrong with configuration, logging this and falling back to default behaviour
            return true;
        }

        return true;
    }

    /**
     * Is username allowed for identity provider users (Okta)
     */
    public boolean isUsernameAllowedForOkta(String username) {
        if (StringUtils.isBlank(username)) {
            return true;
        }
        return !spUsernames.contains(username);
    }

    public boolean isInSPGroups(Collection<String> userGroups) {
        if (userGroups == null || userGroups.isEmpty() || spGroupnames.isEmpty()) {
            return false;
        }

        for (String atlGroup: spGroupnames) {
            for (String userGroup : userGroups) {
                if (userGroup.trim().equalsIgnoreCase(atlGroup.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSPUsernamesUsed() {
        return !spUsernames.isEmpty();
    }

    public boolean isSPGroupnamesUsed() {
        return !spGroupnames.isEmpty();
    }

    /**
     * Is ip allowed for service provider users
     */
    public boolean isIpAllowedForSP(String ip) {
       return !isIpAllowedForOkta(ip);
    }

    public boolean suppressingErrors() {
        return suppressErrors;
    }

    public String getLoginUri() {
        return loginUri;
    }
}