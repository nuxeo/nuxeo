/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ComponentTagUtils.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.facelets.FaceletContext;

/**
 * Component tag utils.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class ComponentTagUtils {

    private static final Log log = LogFactory.getLog(ComponentTagUtils.class);

    // Utility class.
    private ComponentTagUtils() {
    }

    /**
     * Returns true if the specified value conforms to the syntax requirements
     * of a value binding expression.
     *
     * @param value the value to evaluate (not null)
     */
    public static boolean isValueReference(String value) {
        if (value == null) {
            return false;
        }
        return value.contains("#{") && value.indexOf("#{") < value.indexOf('}')
                || value.contains("${")
                && value.indexOf("${") < value.indexOf('}');
    }

    /**
     * Returns true if the specified value conforms to the syntax requirements
     * of a method binding expression.
     * <p>
     * The method can have parameters and the expression must use parentheses
     * even if no parameters are needed.
     *
     * @param value the value to evaluate (not null)
     */
    public static boolean isMethodReference(String value) {
        boolean isValue = isValueReference(value);
        if (isValue) {
            if (value.contains("(")
                    && value.indexOf('(') < value.indexOf(')')
                    // make sure it's not a function
                    && (!value.contains(":") || value.indexOf(':') > value.indexOf('('))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves an expression from a given faces context.
     * <p>
     * Resolves the expression a second time when first resolution gives a
     * String value using the EL Expression syntax.
     * <p>
     * Does not throw any error when resolution fails (only logs an error
     * message).
     *
     * @see #resolveElExpression(FaceletContext, String)
     */
    public static Object resolveElExpression(FacesContext context,
            String elExpression) {
        if (!isValueReference(elExpression)) {
            // literal
            return elExpression;
        } else {
            if (context == null) {
                log.error(String.format(
                        "FacesContext is null => cannot resolve el expression '%s'",
                        elExpression));
                return null;
            }
            // expression => evaluate
            Application app = context.getApplication();
            try {
                return app.evaluateExpressionGet(context, elExpression,
                        Object.class);
            } catch (Exception e) {
                log.error(String.format(
                        "Faces context: Error processing expression '%s'",
                        elExpression), e);
                return null;
            }
        }
    }

    /**
     * Resolves an expression from a given facelet context, using its
     * {@link ExpressionFactory} that can hold a wider context than the faces
     * context behind it.
     * <p>
     * Resolves the expression a second time when first resolution gives a
     * String value using the EL Expression syntax.
     * <p>
     * Does not throw any error when resolution fails (only logs an error
     * message).
     */
    public static Object resolveElExpression(FaceletContext faceletContext,
            String elExpression) {
        if (!isValueReference(elExpression)) {
            // literal
            return elExpression;
        } else {
            if (faceletContext == null) {
                log.error(String.format(
                        "FaceletContext is null => cannot resolve el expression '%s'",
                        elExpression));
                return null;
            }
            // expression => evaluate
            ExpressionFactory eFactory = faceletContext.getExpressionFactory();
            ELContext elContext = faceletContext.getFacesContext().getELContext();
            ValueExpression expr = eFactory.createValueExpression(
                    faceletContext, elExpression, Object.class);
            try {
                return expr.getValue(elContext);
            } catch (Exception e) {
                log.error(String.format(
                        "Facelet context: Error processing expression '%s'",
                        elExpression), e);
                return null;
            }
        }
    }

}
