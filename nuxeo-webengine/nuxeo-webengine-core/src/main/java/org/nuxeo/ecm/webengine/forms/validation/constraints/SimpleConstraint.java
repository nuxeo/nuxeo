/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.forms.validation.constraints;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormInstance;
import org.nuxeo.ecm.webengine.forms.validation.Field;
import org.nuxeo.ecm.webengine.forms.validation.Status;
import org.nuxeo.ecm.webengine.forms.validation.TypeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class SimpleConstraint extends AbstractConstraint {

    private static final Log log = LogFactory.getLog(SimpleConstraint.class);

    // reference another field
    protected String ref;
    protected int index = 0;

    public String getRef() {
        return ref;
    }

    public int getIndex() {
        return index;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public void init(Field field, String value) {
        if (ref == null) {
            if (value == null || value.length() == 0) {
                throw new IllegalArgumentException("Constraint "+getClass().getSimpleName()
                        +" cannot be empty. Form: "+field.getForm()+". Field "+field.getId());
            }
            Object decodedValue = null;
            try {
                decodedValue = field.decode(value);
            } catch (TypeException e) {
                throw new IllegalArgumentException("constraint for "+field.getId()
                        +" must have a value of type "+field.getHandler().getType()+" but is "+value, e);
            }
            doInit(field, value, decodedValue);
        } // else the constraint value is a reference to another field
        // so it will be computed at validation time
    }

    protected abstract void doInit(Field field, String value, Object decodedValue);

    @Override
    public Status validate(FormInstance form, Field field,
            String rawValue, Object value) {
        if (ref != null) {
            String againstValue = getRefValue(form);
            // TODO handle references to multivalued fields
            SimpleConstraint sc = (SimpleConstraint)newInstance();
            sc.errorMessage = errorMessage;
            Object decodedValue = null;
            try {
                decodedValue = field.decode(againstValue);
            } catch (TypeException e) {
                throw new IllegalArgumentException("constraint for "+field.getId()
                        +" must have a value of type "+field.getHandler().getType()+" but is "+value, e);
            }
            sc.doInit(field, againstValue, decodedValue);
            return sc.doValidate(form, field, rawValue, value);
        } else {
            return doValidate(form, field, rawValue, value);
        }
    }

    protected abstract Status doValidate(FormInstance form, Field field,
            String rawValue, Object value);

    protected String getRefValue(FormInstance form) {
        try {
            Object[] values = form.get(ref);
            if (values == null || values.length == 0) {
                return null;
            }
            Object obj = values[index];
            return obj != null ? obj.toString() : null;
        } catch (WebException e) {
            log.error(e);
        }
        return null;
    }

}
