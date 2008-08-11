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

import org.nuxeo.ecm.webengine.forms.FormInstance;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Status {

    public final static Status OK = new Status() {
        public boolean isOk() {
            return true;
        }
        public boolean isMultiStatus() {
            return false;
        }
        public String getField() {
            return null;
        }
        public Status[] getChildren() {
            return null;
        }
        public String getMessage() {
            return "OK";
        }
        public String getParametrizedMessage(FormInstance data) {
            return "OK";
        }
        public Status negate() {
            return KO;
        }
        @Override
        public String toString() {
            return "OK";
        }
        public JSON toJSON() {
            return new JSONObject().element("isOk", true);
        }
        public String toJSONString() {
            return toJSON().toString();
        }
    };

    public final static ErrorStatus KO = new ErrorStatus(null) {
    };

    boolean isOk();

    boolean isMultiStatus();

    String getField(); // XXX must return a Field instead ?

    String getMessage();

    Status[] getChildren();

    String getParametrizedMessage(FormInstance data);

    Status negate();

    JSON toJSON();

    String toJSONString();

}
