/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 *     Julien Carsique
 *
 */

package org.nuxeo.ecm.admin.setup;

import static org.nuxeo.common.Environment.NUXEO_DATA_DIR;
import static org.nuxeo.common.Environment.NUXEO_LOG_DIR;
import static org.nuxeo.common.Environment.PRODUCT_NAME;
import static org.nuxeo.common.Environment.PRODUCT_VERSION;
import static org.nuxeo.launcher.config.ConfigurationGenerator.NUXEO_CONF;
import static org.nuxeo.launcher.config.ConfigurationGenerator.NUXEO_DEV_SYSTEM_PROP;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_BIND_ADDRESS;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_HOST;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_NAME;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_PORT;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_PWD;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_USER;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_NUXEO_URL;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_TEMPLATE_DBNAME;
import static org.nuxeo.launcher.config.ConfigurationGenerator.SECRET_KEYS;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.validator.ValidatorException;
import javax.naming.AuthenticationException;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.launcher.commons.DatabaseDriverException;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * Serves UI for the setup screen, handling properties that can be saved on the bin/nuxeo.conf file on server.
 * <p>
 * Manages some important parameters to perform validation on them, and accepts custom parameters that would be present
 * in the server nuxeo.conf file, and moves to advanced mode any property that would not be in that list.
 *
 * @since 5.5
 */
@Scope(ScopeType.SESSION)
@Name("setupWizardAction")
public class SetupWizardActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(SetupWizardActionBean.class);

    /**
     * The list of important parameters that need to be presented first to the user
     */
    private static final String[] managedKeyParameters = { PARAM_BIND_ADDRESS, PARAM_NUXEO_URL, NUXEO_DATA_DIR,
            NUXEO_LOG_DIR, PRODUCT_NAME, PRODUCT_VERSION, NUXEO_CONF, PARAM_TEMPLATE_DBNAME, PARAM_DB_NAME,
            PARAM_DB_USER, PARAM_DB_PWD, PARAM_DB_HOST, PARAM_DB_PORT, "nuxeo.db.min-pool-size",
            "nuxeo.db.min-pool-size", "nuxeo.db.max-pool-size", "nuxeo.vcs.min-pool-size", "nuxeo.vcs.max-pool-size",
            "nuxeo.notification.eMailSubjectPrefix", "mailservice.user", "mailservice.password", "mail.store.protocol",
            "mail.transport.protocol", "mail.store.host", "mail.store.port", "mail.store.user", "mail.store.password",
            "mail.debug", "mail.transport.host", "mail.transport.port", "mail.transport.auth", "mail.transport.user",
            "mail.transport.password", "mail.from", "mail.user", "mail.transport.usetls", "nuxeo.http.proxy.host",
            "nuxeo.http.proxy.port", "nuxeo.http.proxy.login", "nuxeo.http.proxy.password", NUXEO_DEV_SYSTEM_PROP,
            "nuxeo.directory.type", "nuxeo.user.group.storage", "nuxeo.ldap.url", "nuxeo.ldap.binddn",
            "nuxeo.ldap.bindpassword", "nuxeo.ldap.retries", "nuxeo.ldap.user.searchBaseDn",
            "nuxeo.ldap.user.searchClass", "nuxeo.ldap.user.searchFilter", "nuxeo.ldap.user.searchScope",
            "nuxeo.ldap.user.readonly", "nuxeo.ldap.user.mapping.rdn", "nuxeo.ldap.user.mapping.username",
            "nuxeo.ldap.user.mapping.password", "nuxeo.ldap.user.mapping.firstname", "nuxeo.ldap.user.mapping.lastname",
            "nuxeo.ldap.user.mapping.email", "nuxeo.ldap.user.mapping.company", "nuxeo.ldap.group.searchBaseDn",
            "nuxeo.ldap.group.searchFilter", "nuxeo.ldap.group.searchScope", "nuxeo.ldap.group.readonly",
            "nuxeo.ldap.group.mapping.rdn", "nuxeo.ldap.group.mapping.name", "nuxeo.ldap.group.mapping.label",
            "nuxeo.ldap.group.mapping.members.staticAttributeId", "nuxeo.ldap.group.mapping.members.dynamicAttributeId",
            "nuxeo.ldap.defaultAdministratorId", "nuxeo.ldap.defaultMembersGroup", "nuxeo.user.anonymous.enable",
            "nuxeo.user.emergency.enable", "nuxeo.user.emergency.username", "nuxeo.user.emergency.password",
            "nuxeo.user.emergency.firstname", "nuxeo.user.emergency.lastname" };

    protected Map<String, Serializable> parameters;

    protected Map<String, Serializable> advancedParameters;

    protected static final String PROXY_NONE = "none";

    protected static final String PROXY_ANONYMOUS = "anonymous";

    protected static final String PROXY_AUTHENTICATED = "authenticated";

    protected static final String DIRECTORY_DEFAULT = "default";

    protected static final String DIRECTORY_LDAP = "ldap";

    protected static final String DIRECTORY_MULTI = "multi";

    private static final String ERROR_DB_DRIVER = "error.db.driver.notfound";

    private static final String ERROR_DB_CONNECTION = "error.db.connection";

    private static final String ERROR_LDAP_CONNECTION = "error.ldap.connection";

    private static final String ERROR_LDAP_AUTHENTICATION = "error.ldap.authentication";

    private static final String ERROR_DB_FS = "error.db.fs";

    protected String proxyType = PROXY_NONE;

    protected String directoryType = DIRECTORY_DEFAULT;

    protected boolean needsRestart = false;

    @In(create = true)
    private transient ConfigurationGenerator setupConfigGenerator;

    protected Properties userConfig;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    private Boolean needGroupConfiguration;

    @Factory(value = "setupRequiresRestart", scope = ScopeType.EVENT)
    public boolean isNeedsRestart() {
        return needsRestart;
    }

    public void setNeedsRestart(boolean needsRestart) {
        this.needsRestart = needsRestart;
    }

    @Factory(value = "setupConfigGenerator", scope = ScopeType.PAGE)
    public ConfigurationGenerator getConfigurationGenerator() {
        if (setupConfigGenerator == null) {
            setupConfigGenerator = new ConfigurationGenerator();
            if (setupConfigGenerator.init()) {
                setParameters();
            }
        }
        return setupConfigGenerator;
    }

    @Factory(value = "setupConfigurable", scope = ScopeType.APPLICATION)
    public boolean isConfigurable() {
        return setupConfigGenerator.isConfigurable();
    }

    @Factory(value = "advancedParams", scope = ScopeType.EVENT)
    public Map<String, Serializable> getAdvancedParameters() {
        return advancedParameters;
    }

    @Factory(value = "setupParams", scope = ScopeType.EVENT)
    public Map<String, Serializable> getParameters() {
        return parameters;
    }

    /**
     * Fill {@link #parameters} and {@link #advancedParameters} with properties from #
     * {@link ConfigurationGenerator#getUserConfig()}
     *
     * @since 5.6
     */
    protected void setParameters() {
        userConfig = setupConfigGenerator.getUserConfig();
        parameters = new HashMap<>();
        advancedParameters = new TreeMap<>();
        // will remove managed parameters later in setParameter()
        for (String key : userConfig.stringPropertyNames()) {
            if (System.getProperty(key) == null || key.matches("^(nuxeo|org\\.nuxeo|catalina|derby|h2|java\\.home|"
                    + "java\\.io\\.tmpdir|tomcat|sun\\.rmi\\.dgc).*")) {
                advancedParameters.put(key, userConfig.getProperty(key).trim());
            }
        }
        for (String keyParam : managedKeyParameters) {
            String parameter = userConfig.getProperty(keyParam);
            setParameter(keyParam, parameter);
        }

        proxyType = PROXY_NONE;
        if (parameters.get("nuxeo.http.proxy.host") != null) {
            proxyType = PROXY_ANONYMOUS;
            if (parameters.get("nuxeo.http.proxy.login") != null) {
                proxyType = PROXY_AUTHENTICATED;
            }
        }

        if (parameters.get("nuxeo.directory.type") != null) {
            directoryType = (String) parameters.get("nuxeo.directory.type");
        }
    }

    /**
     * Adds parameter value to the
     *
     * @param key parameter key such as used in templates and nuxeo.conf
     */
    private void setParameter(String key, String value) {
        if (value != null) {
            parameters.put(key, value.trim());
            advancedParameters.remove(key);
        }
    }

    public void save() {
        saveParameters();
        setNeedsRestart(true);
        resetParameters();
        // initialize setupConfigurator again, as it's in scope page
        getConfigurationGenerator();
        facesMessages.add(StatusMessage.Severity.INFO, messages.get("label.parameters.saved"));
    }

    @SuppressWarnings("unchecked")
    protected void saveParameters() {
        // manage httpProxy settings (setting null is not accepted)
        if (!PROXY_AUTHENTICATED.equals(proxyType)) {
            parameters.put("nuxeo.http.proxy.login", "");
            parameters.put("nuxeo.http.proxy.password", "");
        }
        if (PROXY_NONE.equals(proxyType)) {
            parameters.put("nuxeo.http.proxy.host", "");
            parameters.put("nuxeo.http.proxy.port", "");
        }

        // Remove empty values for password keys
        for (String pwdKey : SECRET_KEYS) {
            if (StringUtils.isEmpty((String) parameters.get(pwdKey))) {
                parameters.remove(pwdKey);
            }
        }

        // compute <String, String> parameters for the ConfigurationGenerator
        Stream<Entry<String, Serializable>> parametersStream = parameters.entrySet().stream();
        Stream<Entry<String, Serializable>> advancedParametersStream = advancedParameters.entrySet().stream();
        Map<String, String> customParameters = Stream.concat(parametersStream, advancedParametersStream).collect(
                HashMap::new, (m, e) -> m.put(e.getKey(), Optional.ofNullable(e.getValue())
                                                                  .map(Object::toString)
                                                                  .orElse(null)),
                HashMap::putAll);
        try {
            setupConfigGenerator.saveFilteredConfiguration(customParameters);
        } catch (ConfigurationException e) {
            log.error(e, e);
        }
    }

    public void resetParameters() {
        setupConfigGenerator = null;
        parameters = null;
        advancedParameters = null;
        Contexts.getPageContext().remove("setupConfigGenerator");
    }

    /**
     * @since 5.6
     */
    public void checkDatabaseParameters(FacesContext context, UIComponent component, Object value) {
        Map<String, Object> attributes = component.getAttributes();
        String dbNameInputId = (String) attributes.get("dbNameInputId");
        String dbUserInputId = (String) attributes.get("dbUserInputId");
        String dbPwdInputId = (String) attributes.get("dbPwdInputId");
        String dbHostInputId = (String) attributes.get("dbHostInputId");
        String dbPortInputId = (String) attributes.get("dbPortInputId");

        if (dbNameInputId == null || dbUserInputId == null || dbPwdInputId == null || dbHostInputId == null
                || dbPortInputId == null) {
            log.error("Cannot validate database parameters: missing inputIds");
            return;
        }

        UIInput dbNameComp = (UIInput) component.findComponent(dbNameInputId);
        UIInput dbUserComp = (UIInput) component.findComponent(dbUserInputId);
        UIInput dbPwdComp = (UIInput) component.findComponent(dbPwdInputId);
        UIInput dbHostComp = (UIInput) component.findComponent(dbHostInputId);
        UIInput dbPortComp = (UIInput) component.findComponent(dbPortInputId);
        if (dbNameComp == null || dbUserComp == null || dbPwdComp == null || dbHostComp == null || dbPortComp == null) {
            log.error("Cannot validate inputs: not found");
            return;
        }

        String dbName = (String) dbNameComp.getLocalValue();
        String dbUser = (String) dbUserComp.getLocalValue();
        String dbPwd = (String) dbPwdComp.getLocalValue();
        String dbHost = (String) dbHostComp.getLocalValue();
        // widget is of type int but we can get Integer/Long/BigDecimal so cast to Number
        String dbPort = Long.toString(((Number) dbPortComp.getLocalValue()).longValue());

        if (StringUtils.isEmpty(dbPwd)) {
            dbPwd = (String) parameters.get("nuxeo.db.password");
        }

        String errorLabel = null;
        Exception error = null;
        try {
            setupConfigGenerator.checkDatabaseConnection(
                    (String) parameters.get(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME), dbName, dbUser, dbPwd,
                    dbHost, dbPort);
        } catch (IOException e) {
            errorLabel = ERROR_DB_FS;
            error = e;
        } catch (DatabaseDriverException e) {
            errorLabel = ERROR_DB_DRIVER;
            error = e;
        } catch (SQLException e) {
            errorLabel = ERROR_DB_CONNECTION;
            error = e;
        }
        if (error != null) {
            log.error(error, error);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context, errorLabel), null);
            throw new ValidatorException(message);
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                ComponentUtils.translate(context, "error.db.none"), null);
        message.setSeverity(FacesMessage.SEVERITY_INFO);
        context.addMessage(component.getClientId(context), message);
    }

    public void templateChange(AjaxBehaviorEvent event) {
        String dbTemplate;
        UIComponent select = event.getComponent();
        if (select instanceof ValueHolder) {
            dbTemplate = (String) ((ValueHolder) select).getValue();
        } else {
            log.error("Bad component returned " + select);
            throw new AbortProcessingException("Bad component returned " + select);
        }
        setupConfigGenerator.changeDBTemplate(dbTemplate);
        setParameters();
        Contexts.getEventContext().remove("setupParams");
        Contexts.getEventContext().remove("advancedParams");
        FacesContext context = FacesContext.getCurrentInstance();
        context.renderResponse();
    }

    public void proxyChange(AjaxBehaviorEvent event) {
        UIComponent select = event.getComponent();
        if (select instanceof ValueHolder) {
            proxyType = (String) ((ValueHolder) select).getValue();
        } else {
            log.error("Bad component returned " + select);
            throw new AbortProcessingException("Bad component returned " + select);
        }
        Contexts.getEventContext().remove("setupParams");
        FacesContext context = FacesContext.getCurrentInstance();
        context.renderResponse();
    }

    /**
     * Initialized by {@link #getParameters()}
     */
    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public String getDirectoryType() {
        return directoryType;
    }

    public void setDirectoryType(String directoryType) {
        parameters.put("nuxeo.directory.type", directoryType);
        this.directoryType = directoryType;
    }

    public void setDirectoryStorage(String directoryStorage) {
        parameters.put("nuxeo.user.group.storage", directoryStorage);
    }

    public void ldapStorageChange() {
        needGroupConfiguration = null;
    }

    public boolean getNeedGroupConfiguration() {
        if (needGroupConfiguration == null) {
            String storageType = (String) parameters.get("nuxeo.user.group.storage");
            if ("userLdapOnly".equals(storageType) || "multiUserSqlGroup".equals(storageType)) {
                needGroupConfiguration = Boolean.FALSE;
            } else {
                needGroupConfiguration = Boolean.TRUE;
            }
        }
        return needGroupConfiguration;
    }

    public void directoryChange(AjaxBehaviorEvent event) {
        UIComponent select = event.getComponent();
        if (select instanceof ValueHolder) {
            directoryType = (String) ((ValueHolder) select).getValue();
        } else {
            log.error("Bad component returned " + select);
            throw new AbortProcessingException("Bad component returned " + select);
        }
        if ("multi".equals(directoryType)) {
            setDirectoryStorage("multiUserGroup");
        } else {
            setDirectoryStorage("default");
        }
        needGroupConfiguration = null;
        Contexts.getEventContext().remove("setupParams");
        FacesContext context = FacesContext.getCurrentInstance();
        context.renderResponse();
    }

    public void checkLdapNetworkParameters(FacesContext context, UIComponent component, Object value) {
        Map<String, Object> attributes = component.getAttributes();
        String ldapUrlId = (String) attributes.get("directoryLdapUrl");

        if (ldapUrlId == null) {
            log.error("Cannot validate LDAP parameters: missing inputIds");
            return;
        }

        UIInput ldapUrlComp = (UIInput) component.findComponent(ldapUrlId);
        if (ldapUrlComp == null) {
            log.error("Cannot validate LDAP inputs: not found");
            return;
        }

        String ldapUrl = (String) ldapUrlComp.getLocalValue();

        String errorLabel = null;
        Exception error = null;
        try {
            setupConfigGenerator.checkLdapConnection(ldapUrl, null, null, false);
        } catch (NamingException e) {
            errorLabel = ERROR_LDAP_CONNECTION;
            error = e;
        }
        if (error != null) {
            log.error(error, error);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context, errorLabel), null);
            throw new ValidatorException(message);
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                ComponentUtils.translate(context, "error.ldap.network.none"), null);
        message.setSeverity(FacesMessage.SEVERITY_INFO);
        context.addMessage(component.getClientId(context), message);
    }

    public void checkLdapAuthenticationParameters(FacesContext context, UIComponent component, Object value) {

        Map<String, Object> attributes = component.getAttributes();
        String ldapUrlId = (String) attributes.get("ldapUrl");
        String ldapBinddnId = (String) attributes.get("ldapBindDn");
        String ldapBindpwdId = (String) attributes.get("ldapBindPwd");

        if (ldapUrlId == null || ldapBinddnId == null || ldapBindpwdId == null) {
            log.error("Cannot validate LDAP parameters: missing inputIds");
            return;
        }

        UIInput ldapUrlComp = (UIInput) component.findComponent(ldapUrlId);
        UIInput ldapBinddnComp = (UIInput) component.findComponent(ldapBinddnId);
        UIInput ldapBindpwdComp = (UIInput) component.findComponent(ldapBindpwdId);
        if (ldapUrlComp == null || ldapBinddnComp == null || ldapBindpwdComp == null) {
            log.error("Cannot validate LDAP inputs: not found");
            return;
        }

        String ldapUrl = (String) ldapUrlComp.getLocalValue();
        String ldapBindDn = (String) ldapBinddnComp.getLocalValue();
        String ldapBindPwd = (String) ldapBindpwdComp.getLocalValue();

        String errorLabel = null;
        Exception error = null;
        try {
            setupConfigGenerator.checkLdapConnection(ldapUrl, ldapBindDn, ldapBindPwd, true);
        } catch (NamingException e) {
            if (e instanceof AuthenticationException) {
                errorLabel = ERROR_LDAP_AUTHENTICATION;
            } else {
                errorLabel = ERROR_LDAP_CONNECTION;
            }
            error = e;
        }
        if (error != null) {
            log.error(error, error);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context, errorLabel), null);
            throw new ValidatorException(message);
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                ComponentUtils.translate(context, "error.ldap.auth.none"), null);
        message.setSeverity(FacesMessage.SEVERITY_INFO);
        context.addMessage(component.getClientId(context), message);
    }

}
