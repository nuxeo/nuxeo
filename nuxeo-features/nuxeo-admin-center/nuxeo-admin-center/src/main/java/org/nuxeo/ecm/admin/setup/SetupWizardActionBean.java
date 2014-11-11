/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.setup;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;

@Scope(ScopeType.SESSION)
@Name("setupWizardAction")
public class SetupWizardActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(SetupWizardActionBean.class);

    private static final String[] managedKeyParameters = new String[] {
            "nuxeo.bind.address", "nuxeo.url", "nuxeo.data.dir",
            "nuxeo.log.dir", "org.nuxeo.ecm.product.name",
            "org.nuxeo.ecm.product.version", "nuxeo.conf",
            ConfigurationGenerator.PARAM_TEMPLATE_DBNAME, "nuxeo.db.name",
            "nuxeo.db.user", "nuxeo.db.password", "nuxeo.db.host",
            "nuxeo.db.port", "nuxeo.db.min-pool-size",
            "nuxeo.db.min-pool-size", "nuxeo.db.max-pool-size",
            "nuxeo.vcs.min-pool-size", "nuxeo.vcs.max-pool-size",
            "nuxeo.notification.eMailSubjectPrefix", "mailservice.user",
            "mailservice.password", "mail.store.protocol",
            "mail.transport.protocol", "mail.pop3.host", "mail.debug",
            "mail.smtp.host", "mail.smtp.port", "mail.smtp.auth",
            "mail.smtp.username", "mail.smtp.password", "mail.from" };

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    protected Map<String, String> parameters = null;

    protected Map<String, String> advancedParameters = null;

    @Factory(value = "advancedParams", scope = ScopeType.STATELESS)
    public Map<String, String> getAdvancedParameters() {
        if (advancedParameters == null) {
            readParameters();
        }
        return advancedParameters;
    }

    // @Factory(value = "dbtemplates", scope = ScopeType.APPLICATION)
    // public List<String> getDBTemplates() {
    // return ConfigurationGenerator.DB_LIST;
    // }

    // protected List<String> dbTemplatesLabels = null;

    // @Factory(value = "dbtemplatesLabels", scope = ScopeType.APPLICATION)
    // public List<String> getDBTemplatesLabels() {
    // if (dbTemplatesLabels == null) {
    // dbTemplatesLabels = new ArrayList<String>(
    // ConfigurationGenerator.DB_LIST.size());
    // for (String templateName : ConfigurationGenerator.DB_LIST) {
    // dbTemplatesLabels.add(resourcesAccessor.getMessages().get(
    // "label.setup.nuxeo.template." + templateName));
    // }
    // }
    // return dbTemplatesLabels;
    // }

    protected static boolean needsRestart = false;

    protected boolean configurable = false;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @Factory(value = "configurable", scope = ScopeType.SESSION)
    public boolean isConfigurable() {
        if (configGenerator == null) {
            readParameters();
        }
        return configurable;
    }

    private ConfigurationGenerator configGenerator;

    protected Properties userConfig;

    @Factory(value = "setupRequiresRestart", scope = ScopeType.EVENT)
    public boolean isNeedsRestart() {
        return needsRestart;
    }

    @Factory(value = "setupParams", scope = ScopeType.STATELESS)
    public Map<String, String> getParameters() {
        if (parameters == null) {
            readParameters();
        }
        return parameters;
    }

    protected void readParameters() {
        configGenerator = new ConfigurationGenerator();
        configGenerator.init();
        configurable = configGenerator.isConfigurable();
        if (configurable) {
            setParameters();
        } else {
            log.debug("Server not configurable !");
        }
    }

    private void setParameters() {
        userConfig = configGenerator.getUserConfig();
        parameters = new HashMap<String, String>();
        advancedParameters = new TreeMap<String, String>();
        // will remove managed parameters later, let only advanced parameters
        for (String key : userConfig.stringPropertyNames()) {
            advancedParameters.put(key, userConfig.getProperty(key).trim());
        }
        for (String keyParam : managedKeyParameters) {
            setParameter(keyParam);
        }
    }

    /**
     * @param key parameter key such as used in templates and nuxeo.conf
     */
    private void setParameter(String key) {
        String parameter = userConfig.getProperty(key);
        if (parameter != null) {
            parameters.put(key, parameter.trim());
            advancedParameters.remove(key);
        }
    }

    public void save() {
        saveParameters();
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("label.parameters.saved"));
        needsRestart = true;
        resetParameters();
    }

    protected void saveParameters() {
        // Calculates new templates string
        String currentDB = parameters.get(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME);
        advancedParameters.put(ConfigurationGenerator.PARAM_TEMPLATES_NAME,
                rebuildTemplatesStr(currentDB));

        // Build a Map of changed parameters for ConfigurationGenerator
        Map<String, String> changedParameters = new HashMap<String, String>();
        for (String key : parameters.keySet()) {
            String oldParam = userConfig.getProperty(key);
            String newParam = parameters.get(key).trim();
            if (oldParam == null && !newParam.isEmpty() || oldParam != null
                    && !oldParam.trim().equals(newParam)) {
                changedParameters.put(key, parameters.get(key).trim());
            }
        }
        for (String key : advancedParameters.keySet()) {
            String oldParam = userConfig.getProperty(key);
            String newParam = advancedParameters.get(key).trim();
            if (oldParam == null && !newParam.isEmpty() || oldParam != null
                    && !oldParam.trim().equals(newParam)) {
                changedParameters.put(key, advancedParameters.get(key).trim());
            }
        }
        try {
            configGenerator.saveConfiguration(changedParameters);
        } catch (ConfigurationException e) {
            log.error(e);
        }
    }

    public void resetParameters() {
        readParameters();
        Contexts.getPageContext().remove("setupParams");
        Contexts.getPageContext().remove("advancedParams");
        Contexts.getEventContext().remove("setupRequiresRestart");
    }

    public void templateChange(ActionEvent event) {
        String dbTemplate;
        UIComponent select = event.getComponent().getParent();
        if (select instanceof ValueHolder) {
            dbTemplate = (String) ((ValueHolder) select).getValue();
        } else {
            log.error("Bad component returned " + select);
            throw new AbortProcessingException("Bad component returned "
                    + select);
        }
        configGenerator.changeTemplates(rebuildTemplatesStr(dbTemplate));
        setParameters();
        Contexts.getPageContext().remove("setupParams");
        Contexts.getPageContext().remove("advancedParams");
        FacesContext context = FacesContext.getCurrentInstance();
        context.renderResponse();
    }

    private String rebuildTemplatesStr(String dbTemplate) {
        String nodbTemplates = advancedParameters.get(ConfigurationGenerator.PARAM_TEMPLATES_NODB);
        String templates = nodbTemplates.isEmpty() ? dbTemplate : dbTemplate
                + "," + nodbTemplates;
        return templates;
    }

}
