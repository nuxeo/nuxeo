/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.shibboleth.computedgroups;

import javax.el.ELException;
import javax.el.PropertyNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethConstants;

/**
 * Helper to provide an easy way to execute the expression language defined in a Shibb Group doc
 *
 * @author Arnaud Kervern
 */
public class ELGroupComputerHelper {

    private static final Log log = LogFactory.getLog(ELGroupComputerHelper.class);

    protected static final ExpressionContext ec = new ExpressionContext();

    protected static final ExpressionEvaluator ee = new ExpressionEvaluator(new ExpressionFactoryImpl());

    public static boolean isUserInGroup(DocumentModel user, String el) {
        if (el == null || el.equals("")) {
            return false;
        }

        ee.bindValue(ec, ShibbolethConstants.EL_CURRENT_USER_NAME, user);
        return ee.evaluateExpression(ec, "${" + el + "}", Boolean.class);
    }

    public static boolean isValidEL(String el) {
        if (el == null || el.equals("")) {
            return false;
        }

        try {
            ee.bindValue(ec, ShibbolethConstants.EL_CURRENT_USER_NAME, new DocumentModelImpl("user"));
            ee.evaluateExpression(ec, "${" + el + "}", Boolean.class);
        } catch (PropertyNotFoundException e) {
            return false;
        } catch (ELException e) {
            log.error(e, e);
            return false;
        }
        return true;
    }
}
