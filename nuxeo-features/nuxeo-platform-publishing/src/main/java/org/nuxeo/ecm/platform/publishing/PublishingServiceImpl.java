/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: PublishingServiceImpl.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.publishing.api.PublishingValidatorException;
import org.nuxeo.ecm.platform.publishing.api.ValidatorsRule;
import org.nuxeo.ecm.platform.publishing.rules.ValidatorsRuleDesc;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Publishing service implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class PublishingServiceImpl extends DefaultComponent implements
        PublishingService {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PublishingServiceImpl.class);

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.publishing");

    private static final String PT_VALIDATORS_RULE = "publishingValidatorsRule";

    private static final String PT_VALID_DATE_FIELD = "publishingValidDateField";

    protected PublishingValidDateFieldDesc prefixed;

    protected ValidatorsRuleDesc validatorsRuleDesc;

    public ValidatorsRule getValidatorsRule()
            throws PublishingValidatorException {

        if (validatorsRuleDesc == null) {
            throw new PublishingValidatorException(
                    "No publishing validator rule defined...");
        }

        try {
            return validatorsRuleDesc.getKlass().newInstance();
        } catch (InstantiationException e) {
            throw new PublishingValidatorException(e);
        } catch (IllegalAccessException e) {
            throw new PublishingValidatorException(e);
        }

    }

    public String getValidDateFieldName() {
        return prefixed.getPrefixedFieldName().split(":")[1];
    }

    public String getValidDateFieldSchemaPrefixName() {
        return prefixed.getPrefixedFieldName().split(":")[0];
    }

    public String[] getValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException {
        return getValidatorsRule().computesValidatorsFor(dm);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(PT_VALIDATORS_RULE)) {

            validatorsRuleDesc = (ValidatorsRuleDesc) contribution;
            log.info("Registered publishing validator : "
                    + validatorsRuleDesc.getClass().getCanonicalName()
                    + " Previous registration is overriden.");

        } else if (extensionPoint.equals(PT_VALID_DATE_FIELD)) {

            prefixed = (PublishingValidDateFieldDesc) contribution;
            log.info("Registered publishing valid date field : "
                    + prefixed.getPrefixedFieldName()
                    + " Previous registration is overriden");

        } else {
            log.error("Extension point name is unknown... => " + extensionPoint);
        }

    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(PT_VALIDATORS_RULE)) {
            validatorsRuleDesc = null;
            log.info("Publishing validators rule unregistered !");
        } else if (extensionPoint.equals(PT_VALID_DATE_FIELD)) {
            prefixed = null;
            log.info("Publishing valid date field conf unregistred !");
        } else {
            log.error("Extension point name is unknown... => " + extensionPoint);
        }
    }

}
