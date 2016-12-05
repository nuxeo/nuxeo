/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.ecm.automation.context.ContextService;
import org.nuxeo.ecm.automation.core.scripting.DateWrapper;
import org.nuxeo.ecm.automation.core.scripting.PrincipalWrapper;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * Class injected/published in Nashorn engine to execute automation service.
 *
 * @since 7.2
 */
public class AutomationMapper implements Bindings {

    protected final OperationContext ctx;

    protected final Map<String, Supplier<Object>> automatic = new HashMap<>();

    protected final Bindings bindings = new SimpleBindings();

    protected final Map<String, Object> wrapped = new HashMap<>();

    public static CompiledScript compile(Compilable compilable)  {
        try {
            return new ScriptBuilder().build(compilable);
        } catch (ScriptException cause) {
           throw new NuxeoException("Cannot compile mapper initialization script", cause);
        }
    }

    public AutomationMapper(OperationContext ctx) {
        this.ctx = ctx;
        automatic.put("Session", () -> ctx.getCoreSession());
        automatic.put(AutomationScriptingConstants.AUTOMATION_CTX_KEY, () -> ctx.getVars());
        automatic.put(AutomationScriptingConstants.AUTOMATION_MAPPER_KEY, () -> this);
        automatic.put("CurrentUser", () -> new PrincipalWrapper((NuxeoPrincipal) ctx.getPrincipal()));
        automatic.put("currentUser", () -> new PrincipalWrapper((NuxeoPrincipal) ctx.getPrincipal()));
        automatic.put("Env", () -> Framework.getProperties());
        automatic.put("CurrentDate", () -> new DateWrapper());
        // Helpers injection
        ContextService contextService = Framework.getService(ContextService.class);
        Map<String, ContextHelper> helperFunctions = contextService.getHelperFunctions();
        for (String helperFunctionsId : helperFunctions.keySet()) {
            automatic.put(helperFunctionsId, () -> helperFunctions.get(helperFunctionsId));
        }
    }

    public void flush() {
        wrapped.forEach((k, v) -> ctx.put(k, unwrap(v)));
        wrapped.clear();
    }

    public Object unwrap(Object wrapped) {
        return DocumentScriptingWrapper.unwrap(wrapped);
    }

    public Object wrap(Object unwrapped) {
        return DocumentScriptingWrapper.wrap(unwrapped, this);
    }

    public Object executeOperation(String opId, Object input, ScriptObjectMirror parameters) throws Exception {
        flush();
        ctx.setInput(input = DocumentScriptingWrapper.unwrap(input));
        AutomationService automation = Framework.getService(AutomationService.class);
        Class<?> typeof = input == null ? Void.TYPE : input.getClass();
        OperationParameters args = new OperationParameters(opId, DocumentScriptingWrapper.unwrap(parameters));
        Object output = automation.compileChain(typeof, args).invoke(ctx);
        return wrap(output);
    }

    @Override
    public int size() {
        return Stream
                .concat(automatic.keySet().stream(), Stream.concat(bindings.keySet().stream(), ctx.keySet().stream()))
                .distinct().collect(Collectors.counting()).intValue();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return automatic.containsKey(key) || bindings.containsKey(key) || ctx.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return automatic.containsValue(value) || bindings.containsValue(value) || ctx.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return automatic.getOrDefault(key,
                () -> bindings.computeIfAbsent((String) key, k -> wrap(ctx.get(k))))
                .get();
    }

    @Override
    public Object put(String key, Object value) {
        bindings.put(key, value);
        wrapped.put(key, value);
        return value;
    }

    @Override
    public Object remove(Object key) {
        Object wrapped = bindings.remove(key);
        Object unwrapped = ctx.remove(key);
        if (wrapped == null) {
            wrapped = wrap(unwrapped);
        }
        return wrapped;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        bindings.putAll(m);
        wrapped.putAll(m);
    }

    @Override
    public void clear() {
        bindings.clear();
        wrapped.clear();
        ctx.clear();
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Object> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return Optional.ofNullable(get(key)).orElse(defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ? extends Object> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object replace(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ? extends Object> mappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object computeIfPresent(String key,
            BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object merge(String key, Object value,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    public static class ScriptBuilder {

        public CompiledScript build(Compilable compilable) throws ScriptException {
            return compilable.compile(source());
        }

        public String source() {
            StringBuffer sb = new StringBuffer();
            AutomationService as = Framework.getService(AutomationService.class);
            Map<String, List<String>> opMap = new HashMap<>();
            List<String> flatOps = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (OperationType op : as.getOperations()) {
                ids.add(op.getId());
                if (op.getAliases() != null) {
                    Collections.addAll(ids, op.getAliases());
                }
            }
            // Create js object related to operation categories
            for (String id : ids) {
                parseAutomationIDSForScripting(opMap, flatOps, id);
            }
            for (String obName : opMap.keySet()) {
                List<String> ops = opMap.get(obName);
                sb.append("\nvar ").append(obName).append("={};");
                for (String opId : ops) {
                    generateFunction(sb, opId);
                }
            }
            for (String opId : flatOps) {
                generateFlatFunction(sb, opId);
            }
            return sb.toString();
        }

        protected void parseAutomationIDSForScripting(Map<String, List<String>> opMap, List<String> flatOps,
                String id) {
            if (id.split("\\.").length > 2) {
                return;
            }
            int idx = id.indexOf(".");
            if (idx > 0) {
                String obName = id.substring(0, idx);
                List<String> ops = opMap.get(obName);
                if (ops == null) {
                    ops = new ArrayList<>();
                }
                ops.add(id);
                opMap.put(obName, ops);
            } else {
                // Flat operation: no need of category
                flatOps.add(id);
            }
        }

        protected void generateFunction(StringBuffer sb, String opId) {
            sb.append("\n" + replaceDashByUnderscore(opId) + " = function(input,params) {");
            sb.append("\nreturn automation.executeOperation('" + opId + "', input , params);");
            sb.append("\n};");
        }

        protected void generateFlatFunction(StringBuffer sb, String opId) {
            sb.append("\nvar " + replaceDashByUnderscore(opId) + " = function(input,params) {");
            sb.append("\nreturn automation.executeOperation('" + opId + "', input , params);");
            sb.append("\n};");
        }

        protected String replaceDashByUnderscore(String id) {
            return id.replaceAll("[\\s\\-()]", "_");
        }

    }

}
