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
package org.nuxeo.ecm.platform.ui.web.binding.alias;

import java.io.Serializable;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.javaagent.AgentLoader;
import org.nuxeo.runtime.javaagent.ObjectSizer;

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

    protected int nbVars = 0;

    protected static Instrumentation ins;

    public NuxeoAliasBean() {
        super();
    }

    /**
     * Init marked public for NXP-16182.
     *
     * @since 7.1
     */
    @PostConstruct
    public void init() {
        vms = new HashMap<>();
        nbVars = 0;
    }

    @PreDestroy
    public void stop() {
        if (ComponentUtils.isOptimDebugEnabled()) {
            log.error("Nb mappers = " + vms.size());
            log.error("Nb vars = " + nbVars);
            try {
                ObjectSizer sizer = AgentLoader.INSTANCE.getSizer();
                long deepsize = sizer.deepSizeOf(vms);
                log.error("DeepSize = " + deepsize);
            } catch (ExceptionInInitializerError e) {
                log.error("Cannot log object sizes: " + e.getMessage());
            }

        }
        vms = new HashMap<>();
        nbVars = 0;
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
        int nb = vm.vars.size();
        nbVars += nb;
        if (ComponentUtils.isOptimDebugEnabled()) {
            try {
                ObjectSizer sizer = AgentLoader.INSTANCE.getSizer();
                long deepsize = sizer.deepSizeOf(vm);
                if (deepsize > 600000) {
                    log.error("Nb vars = " + nb);
                    log.error("DeepSize = " + deepsize);
                    log.error(vm.getVariables());
                }
            } catch (ExceptionInInitializerError e) {
                log.error("Cannot log object sizes: " + e.getMessage());
            }
        }
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
        AliasVariableMapper vm = vms.remove(id);
        if (vm != null) {
            nbVars -= vm.vars.size();
        }
    }

}
