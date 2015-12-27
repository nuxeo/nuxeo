/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: MetaMethodExpression.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.MethodInfo;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * Meta method expression used to invoke the EL expression that is already the result of a method expression.
 * <p>
 * For instance it is useful to use this expression to provide action links defined in NXActions extensions with links
 * like #{documentAction.createDocument('Domain')}.
 * <p>
 * There is no more than one level of abstraction:
 * <ul>
 * <li>the expression method value can be a standard method expression (with parameters or not);
 * <li>the expression method value can result in another expression method value after being invoke, in which case it is
 * reinvoked again using the same context;
 * <li>no further method invoking will be performed.
 * </ul>
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MetaMethodExpression extends MethodExpression implements Serializable {

    private static final long serialVersionUID = -2721042412903607760L;

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
                    MethodExpression newMeth = factory.createMethodExpression(context, expression, Object.class,
                            new Class[0]);
                    try {
                        res = newMeth.invoke(context, null);
                    } catch (ELException e) {
                        throw e;
                    } catch (RuntimeException e) {
                        throw new ELException(e);
                    }
                } else {
                    res = expression;
                }
            }
        }
        return res;
    }

    // Externalizable interface

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        originalMethodExpression = (MethodExpression) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(originalMethodExpression);
    }

}
