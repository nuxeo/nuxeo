/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.holder;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * View scoped bean keeping values held by {@link UIValueHolder} component instances when their value is set.
 *
 * @since 6.0
 */
// FIXME: annotations do not trigger registration, need to figure out why
// @ViewScoped
// @ManagedBean
public class NuxeoValueHolderBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "nuxeoValueHolderBean";

    private static final Log log = LogFactory.getLog(NuxeoValueHolderBean.class);

    // map of held value by id. id is the component corresponding facelet tag
    // id, the component facelet tag handler ensures a unique relation between
    // the two.
    protected Map<String, Serializable> values;

    public NuxeoValueHolderBean() {
        super();
    }

    /**
     * Init marked public for NXP-16182.
     *
     * @since 7.1
     */
    @PostConstruct
    @PreDestroy
    public void init() {
        values = new HashMap<>();
    }

    public Map<String, Serializable> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public void saveState(UIValueHolder c, Object value) {
        String fid = c.getFaceletId();
        if (fid == null) {
            log.error("Cannot save UIValueHolder state: " + "missing facelet marker id on component attributes");
            return;
        }
        if (value == null || value instanceof Serializable) {
            values.put(fid, (Serializable) value);
        } else {
            log.warn("Value is not serializable, cannot store it in view: " + value);
        }
    }

    public void saveState(UIValueHolder c) {
        saveState(c, c.getValueToExpose());
    }

    public Object getState(String id) {
        return values.get(id);
    }

    /**
     * Returns true if bean holds a value for given id.
     *
     * @since 7.2
     */
    public boolean hasState(String id) {
        return values.containsKey(id);
    }

}
