/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.component.holder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * View scoped bean keeping values held by {@link UIValueHolder} component
 * instances when their value is set.
 *
 * @since 5.9.4-JSF2
 */
// FIXME: annotations do not trigger registration
// @ViewScoped
// @ManagedBean
public class NuxeoValueHolderBean {

    public static final String NAME = "nuxeoValueHolderBean";

    private static final Log log = LogFactory.getLog(NuxeoValueHolderBean.class);

    // map of held value by id. id is the component corresponding facelet tag
    // id, the component facelet tag handler ensures a unique relation between
    // the two.
    protected Map<String, Object> values;

    public NuxeoValueHolderBean() {
        super();
    }

    @PostConstruct
    @PreDestroy
    protected void init() {
        values = new HashMap<>();
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public void saveState(UIValueHolder c, Object value) {
        String fid = c.getFaceletId();
        if (fid == null) {
            log.error("Cannot save UIValueHolder state: "
                    + "missing facelet marker id on component attributes");
            return;
        }
        values.put(fid, value);
    }

    public void saveState(UIValueHolder c) {
        saveState(c, c.getValueToExpose());
    }

    public Object getState(String id) {
        return values.get(id);
    }

}
