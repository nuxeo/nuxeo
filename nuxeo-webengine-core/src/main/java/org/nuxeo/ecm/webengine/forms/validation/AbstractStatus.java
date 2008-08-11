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

package org.nuxeo.ecm.webengine.forms.validation;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormInstance;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractStatus implements Status {

    protected String field;
    protected String message;
    protected boolean isOk = false;

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }


    public String getParametrizedMessage(FormInstance data) {
        String msg = getMessage();
        if (msg == null) {
            return null;
        }
        if (field != null) {
            try {
                Object[] val = data.get(field);
                if (val == null || val.length == 0) {
                    return getMessage();
                }
                //TODO: handle multi values?
                return String.format(msg, val[0]);
            } catch (WebException e) {
                e.printStackTrace();
            }
        }
        return getMessage();
    }

    public Status negate() {
        isOk = !isOk;
        return this;
    }

    public boolean isOk() {
        return isOk;
    }

    public JSON toJSON() {
        JSONObject obj = new JSONObject()
            .element("isOk", isOk)
            .element("field", field)
            .element("message", message);
        if (isMultiStatus()) {
            obj.element("children", ((MultiStatus)this).getChildren());
        }
        return obj;
    }

    public String toJSONString() {
        return toJSON().toString();
    }

}
