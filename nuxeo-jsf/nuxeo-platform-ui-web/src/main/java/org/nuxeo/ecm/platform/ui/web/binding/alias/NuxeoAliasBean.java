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
package org.nuxeo.ecm.platform.ui.web.binding.alias;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Keep alias variable mappers in request on a managed bean.
 * <p>
 * References to these variables mappers will be done from UI.
 *
 * @since 6.0
 */
public class NuxeoAliasBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "nuxeoAliasBean";

    private static final Log log = LogFactory.getLog(NuxeoAliasBean.class);

    protected Map<String, AliasVariableMapper> vms;

    public NuxeoAliasBean() {
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
        vms = new HashMap<>();
    }

    public Map<String, AliasVariableMapper> getValues() {
        return Collections.unmodifiableMap(vms);
    }

    public AliasVariableMapper get(String id) {
        return vms.get(id);
    }

    public void add(AliasVariableMapper vm) {
        if (vm == null) {
            return;
        }
        String id = vm.getId();
        if (id == null && log.isDebugEnabled()) {
            log.debug("Adding alias variable mapper with null id");
        }
        if (vms.containsKey(id) && log.isTraceEnabled()) {
            log.trace(String.format("Overriding alias variable mapper with id '%s'", id));
        }
        vms.put(id, vm);
        if (log.isTraceEnabled()) {
            log.trace(String.format("Expose alias variable mapper with id '%s': %s", id, vm.getVariables()));
        }
    }

    public void remove(String id) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Remove alias variable mapper with id '%s' from request", id));
        }
        if (id == null) {
            log.debug("Remove alias variable mapper with null id");
        }
        vms.remove(id);
    }

}
