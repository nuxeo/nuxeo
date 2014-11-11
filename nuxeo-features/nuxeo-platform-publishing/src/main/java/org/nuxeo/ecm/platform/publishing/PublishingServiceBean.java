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
 * $Id: PublishingServiceBean.java 28957 2008-01-11 13:36:52Z tdelprat $
 */

package org.nuxeo.ecm.platform.publishing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.publishing.api.PublishingValidatorException;
import org.nuxeo.ecm.platform.publishing.api.ValidatorsRule;
import org.nuxeo.runtime.api.Framework;

/**
 * Publishing service session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
//@Stateless
//@Local(PublishingService.class)
//@Remote(PublishingService.class)
public class PublishingServiceBean implements PublishingService {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PublishingServiceBean.class);

    protected PublishingService service;

    private PublishingService getService() {
        if (service == null) {
            service = Framework.getLocalService(PublishingService.class);
        }
        return service;
    }

    public String getValidDateFieldName() {
        String fieldName;
        try {
            fieldName = getService().getValidDateFieldName();
        } catch (Exception e) {
            log.error("Cannot lookup publishing service...", e);
            fieldName = "valid"; // XXX HARDCODED FALLBACK
        }
        return fieldName;
    }

    public String getValidDateFieldSchemaPrefixName() {
        String prefix;
        try {
            prefix = getService().getValidDateFieldSchemaPrefixName();
        } catch (Exception e) {
            log.error("Cannot lookup publishing service...", e);
            prefix = "dc"; // XXX HARDCODED FALLBACK
        }
        return prefix;
    }

    public String[] getValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException {
        try {
            return getService().getValidatorsFor(dm);
        } catch (Exception e) {
            throw new PublishingValidatorException(e);
        }
    }

    public ValidatorsRule getValidatorsRule()
            throws PublishingValidatorException {
        try {
            return getService().getValidatorsRule();
        } catch (Exception e) {
            throw new PublishingValidatorException(e);
        }
    }

}
