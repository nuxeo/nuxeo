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
 * $Id: MetaMethodExpression.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.MethodInfo;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * Meta method expression used to invoke the EL expression that is already the
 * result of a method expression.
 * <p>
 * For instance it is useful to use this expression to provide action links
 * defined in NXActions extensions with links like
 * #{documentAction.createDocument('Domain')}.
 * <p>
 * There is no more than one level of abstraction:
 * <ul>
 * <li>the expression method value can be a standard method expression (with
 * parameters or not);
 * <li>the expression method value can result in another expression method value
 * after being invoke, in which case it is reinvoked again using the same
 * context;
 * <li>no further method invoking will be performed.
 * </ul>
 * 
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MetaMethodExpression extends MethodExpression implements
        Serializable {

    private static final long serialVersionUID = -2721042412903607760L;

    private static final Log log = LogFactory.getLog(MetaMethodExpression.class);

    private MethodExpression originalMethodExpression;

    public MetaMethodExpression(MethodExpression originalMethodExpression) {
        this.originalMethodExpression = originalMethodExpression;
    }

    // Expression interface

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MetaMethodExpression)) {
            return false;
        }
        MetaMethodExpression other = (MetaMethodExpression) obj;
        return originalMethodExpression.equals(other.originalMethodExpression);
    }

    @Override
    public int hashCode() {
        return originalMethodExpression.hashCode();
    }

    @Override
    public String getExpressionString() {
        return originalMethodExpression.getExpressionString();
    }

    @Override
    public boolean isLiteralText() {
        return originalMethodExpression.isLiteralText();
    }

    // MethodExpression interface

    @Override
    public MethodInfo getMethodInfo(ELContext context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object invoke(ELContext context, Object[] params) {
        Object res = null;
        if (originalMethodExpression != null) {
            res = originalMethodExpression.invoke(context, params);
            if (res instanceof String) {
                String expression = (String) res;
                if (ComponentTagUtils.isValueReference(expression)) {
                    FacesContext faces = FacesContext.getCurrentInstance();
                    Application app = faces.getApplication();
                    ExpressionFactory factory = app.getExpressionFactory();
                    MethodExpression newMeth = factory.createMethodExpression(
                            context, expression, Object.class, new Class[0]);
                    try {
                        res = newMeth.invoke(context, null);
                    } catch (Throwable t) {
                        if (t instanceof InvocationTargetException) {
                            // respect the javadoc contract of the overridden
                            // method
                            throw new ELException(t.getCause());
                        } else {
                            throw new ELException(t);
                        }
                    }
                } else {
                    res = expression;
                }
            }
        }
        return res;
    }

    // Externalizable interface

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        originalMethodExpression = (MethodExpression) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(originalMethodExpression);
    }

}
