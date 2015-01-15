/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Request-scoped bean keeping generated ids counters and logics for layouts and widgets.
 *
 * @since 7.2
 */
public class NuxeoLayoutIdManagerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "nuxeoLayoutIdManager";

    private static final Log log = LogFactory.getLog(NuxeoLayoutIdManagerBean.class);

    protected Map<String, State> ids;

    public NuxeoLayoutIdManagerBean() {
        super();
    }

    public String generateUniqueId(String base) {
        String res;
        // strip base of any remnant counter name
        String bareBase = FaceletHandlerHelper.stripUniqueIdBase(FaceletHandlerHelper.generateValidIdString(base));
        Integer count = 0;
        if (ids != null && ids.containsKey(bareBase)) {
            State state = ids.get(bareBase);
            count = state.getCounter() + 1;
            setCounter(bareBase, count);
            res = bareBase + "_" + count;
        } else {
            res = bareBase;
            setCounter(bareBase, count);
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Computed id='%s' for base='%s'", res, bareBase));
        }
        return res;
    }

    public Map<String, State> getIds() {
        return Collections.unmodifiableMap(ids);
    }

    public void resetIds() {
        ids = null;
    }

    protected void setCounter(String id, Integer count) {
        if (ids == null) {
            ids = new HashMap<>();
        }
        State state;
        if (ids.containsKey(id)) {
            state = ids.get(id);
        } else {
            state = new State();
            ids.put(id, state);
        }
        state.setCounter(count);
    }

    private static final class State implements Serializable {

        private static final long serialVersionUID = 1L;

        protected Integer counter;

        public Integer getCounter() {
            return counter;
        }

        public void setCounter(Integer counter) {
            this.counter = counter;
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();

            buf.append("State");
            buf.append(" {");
            buf.append(" counter=");
            buf.append(counter);
            buf.append('}');

            return buf.toString();
        }

    }

}