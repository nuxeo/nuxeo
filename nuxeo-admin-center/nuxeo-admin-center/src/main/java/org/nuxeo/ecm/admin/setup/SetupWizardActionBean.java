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

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.runtime.deployment.preprocessor.ConfigurationException;
import org.nuxeo.runtime.deployment.preprocessor.ConfigurationGenerator;

@Scope(ScopeType.SESSION)
@Name("setupWizardAction")
public class SetupWizardActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(SetupWizardActionBean.class);

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    protected Map<String, String> parameters = null;

    protected Map<String, String> advancedParameters = null;

    @Factory(value = "advancedParams", scope = ScopeType.PAGE)
    public Map<String, String> getAdvancedParameters() {
        if (advancedParameters == null) {
            readParameters();
        }
        return advancedParameters;
    }

    protected static boolean needsRestart = false;

    private ConfigurationGenerator configGenerator;

    protected Properties userConfig;

    @Factory(value = "setupRequiresRestart", scope = ScopeType.EVENT)
    public boolean isNeedsRestart() {
        return needsRestart;
    }

    @Factory(value = "setupParams", scope = ScopeType.PAGE)
    public Map<String, String> getParameters() {
        if (parameters == null) {
            readParameters();
        }
        return parameters;
    }

    protected void readParameters() {
        configGenerator = new ConfigurationGenerator();
        configGenerator.init();
        userConfig = configGenerator.getUserConfig();

        parameters = new HashMap<String, String>();
        advancedParameters = new HashMap<String, String>();
        // will remove managed parameters later, let only advanced parameters
        for (String key : userConfig.stringPropertyNames()) {
            advancedParameters.put(key, userConfig.getProperty(key).trim());
        }

        setParamater("nuxeo.bind.address");
        setParamater("nuxeo.url");
        setParamater("org.nuxeo.ecm.instance.name");
        setParamater("org.nuxeo.ecm.instance.description");

        setParamater("nuxeo.notification.eMailSubjectPrefix");
        setParamater("mailservice.user");
        setParamater("mailservice.password");
        setParamater("mail.store.protocol");
        setParamater("mail.transport.protocol");
        setParamater("mail.pop3.host");
        setParamater("mail.debug");
        setParamater("mail.smtp.host");
        setParamater("mail.smtp.port");
        setParamater("mail.smtp.auth");
        setParamater("mail.smtp.username");
        setParamater("mail.smtp.password");
        setParamater("mail.from");

        setParamater("nuxeo.db.name");
        setParamater("nuxeo.db.user");
        setParamater("nuxeo.db.password");
        setParamater("nuxeo.db.host");
        setParamater("nuxeo.db.port");
        setParamater("nuxeo.db.min-pool-size");
        setParamater("nuxeo.db.max-pool-size");
        setParamater("nuxeo.vcs.min-pool-size");
        setParamater("nuxeo.vcs.max-pool-size");
    }

    /**
     * @param key parameter key such as used in templates and nuxeo.conf
     */
    private void setParamater(String key) {
        parameters.put(key, userConfig.getProperty(key).trim());
        advancedParameters.remove(key);
    }

    public void save() {
        saveParameters();
        facesMessages.add(FacesMessage.SEVERITY_INFO, "label.parameters.saved");
        needsRestart = true;
        resetParameters();
    }

    protected void saveParameters() {
        Map<String, String> changedParameters = new HashMap<String, String>();
        for (String key : parameters.keySet()) {
            if (userConfig.getProperty(key) == null
                    || !userConfig.getProperty(key).trim().equals(
                            parameters.get(key).trim())) {
                changedParameters.put(key, parameters.get(key).trim());
            }
        }
        for (String key : advancedParameters.keySet()) {
            if (userConfig.getProperty(key) == null
                    || !userConfig.getProperty(key).trim().equals(
                            advancedParameters.get(key).trim())) {
                changedParameters.put(key, advancedParameters.get(key).trim());
            }
        }

        try {
            configGenerator.saveConfiguration(changedParameters);
        } catch (ConfigurationException e) {
            log.error(e);
        }
    }

    protected void resetParameters() {
        readParameters();
        Contexts.getPageContext().remove("setupParams");
        Contexts.getPageContext().remove("advancedParams");
        Contexts.getEventContext().remove("setupRequiresRestart");
    }

}
