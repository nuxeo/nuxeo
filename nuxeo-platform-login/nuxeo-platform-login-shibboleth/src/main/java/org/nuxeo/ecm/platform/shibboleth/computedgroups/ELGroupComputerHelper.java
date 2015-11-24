/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Helper to provide an easy way to execute the expression language defined in
 * a Shibb Group doc
 *
 * @author Arnaud Kervern
 */
public class ELGroupComputerHelper {

    private static final Log log = LogFactory.getLog(ELGroupComputerHelper.class);

    protected static final ExpressionContext ec = new ExpressionContext();

    protected static final ExpressionEvaluator ee = new ExpressionEvaluator(
            new ExpressionFactoryImpl());

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
            ee.bindValue(ec, ShibbolethConstants.EL_CURRENT_USER_NAME,
                    new DocumentModelImpl("user"));
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
