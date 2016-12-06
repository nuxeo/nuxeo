/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.OutputCollector;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
public class OperationTypeImpl implements OperationType {

    /**
     * The service that registered the operation
     */
    protected final AutomationService service;

    /**
     * The operation ID - used for lookups.
     */
    protected final String id;

    /**
     * The operation ID Aliases array.
     *
     * @since 7.1
     */
    protected final String[] aliases;

    /**
     * The operation type
     */
    protected final Class<?> type;

    /**
     * Injectable parameters. a map between the parameter name and the Field object
     */
    protected final Map<String, Field> params;

    /**
     * Invocable methods
     */
    protected List<InvokableMethod> methods;

    /**
     * Fields that should be injected from context
     */
    protected List<Field> injectableFields;

    /**
     * The input type of a chain/operation. If set, the following input types {"document", "documents", "blob", "blobs"}
     * for all 'run method(s)' will handled. Other values will be adapted as java.lang.Object. If not set, Automation
     * will set the input type(s) as the 'run methods(s)' parameter types (by introspection).
     *
     * @since 7.4
     */
    protected String inputType;

    protected String contributingComponent;

    protected List<WidgetDefinition> widgetDefinitionList;

    public OperationTypeImpl(AutomationService service, Class<?> type) {
        this(service, type, null);
    }

    public OperationTypeImpl(AutomationService service, Class<?> type, String contributingComponent) {
        this(service, type, contributingComponent, null);
    }

    /**
     * @since 5.9.5
     */
    public OperationTypeImpl(AutomationService service, Class<?> type, String contributingComponent,
            List<WidgetDefinition> widgetDefinitionList) {
        Operation anno = type.getAnnotation(Operation.class);
        if (anno == null) {
            throw new IllegalArgumentException(
                    "Invalid operation class: " + type + ". No @Operation annotation found on class.");
        }
        this.service = service;
        this.type = type;
        this.widgetDefinitionList = widgetDefinitionList;
        this.contributingComponent = contributingComponent;
        id = anno.id().length() == 0 ? type.getName() : anno.id();
        aliases = anno.aliases();
        params = new HashMap<String, Field>();
        methods = new ArrayList<InvokableMethod>();
        injectableFields = new ArrayList<Field>();
        initMethods();
        initFields();
    }

    static class Match implements Comparable<Match> {
        protected InvokableMethod method;

        int priority;

        Match(InvokableMethod method, int priority) {
            this.method = method;
            this.priority = priority;
        }

        @Override
        public int compareTo(Match o) {
            return o.priority - priority;
        }

        @Override
        public String toString() {
            return "Match(" + method + ", " + priority + ")";
        }
    }

    @Override
    public AutomationService getService() {
        return service;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String[] getAliases() {
        return aliases;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public String getInputType() {
        return inputType;
    }

    protected void initMethods() {
        for (Method method : type.getMethods()) {
            OperationMethod anno = method.getAnnotation(OperationMethod.class);
            if (anno == null) { // skip method
                continue;
            }
            // register regular method
            InvokableMethod im = new InvokableMethod(this, method, anno);
            methods.add(im);
            // check for iterable input support
            if (anno.collector() != OutputCollector.class) {
                // an iterable method - register it
                im = new InvokableIteratorMethod(this, method, anno);
                methods.add(im);
            }
        }
        // method order depends on the JDK, make it deterministic
        Collections.sort(methods);
    }

    protected void initFields() {
        for (Field field : type.getDeclaredFields()) {
            Param param = field.getAnnotation(Param.class);
            if (param != null) {
                field.setAccessible(true);
                params.put(param.name(), field);
            } else if (field.isAnnotationPresent(Context.class)) {
                field.setAccessible(true);
                injectableFields.add(field);
            }
        }
    }

    @Override
    public Object newInstance(OperationContext ctx, Map<String, ?> args) throws OperationException {
        Object obj;
        try {
            obj = type.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new OperationException(e);
        }
        inject(ctx, args, obj);
        return obj;
    }

    /**
     * @since 5.9.2
     */
    protected Object resolveObject(final OperationContext ctx, final String key, Map<String, ?> args) {
        Object obj = args.get(key);
        if (obj instanceof Expression) {
            obj = ((Expression) obj).eval(ctx);
        }
        // Trying to fallback on Chain Parameters sub context if cannot
        // find it
        if (obj == null) {
            if (ctx.containsKey(Constants.VAR_RUNTIME_CHAIN)) {
                @SuppressWarnings("unchecked")
                final Map<String, ?> params = (Map<String, ?>) ctx.get(Constants.VAR_RUNTIME_CHAIN);
                obj = params.get(key);
            }
        }
        return obj;
    }

    public void inject(OperationContext ctx, Map<String, ?> args, Object target) throws OperationException {
        for (Map.Entry<String, Field> entry : params.entrySet()) {
            Object obj = resolveObject(ctx, entry.getKey(), args);
            if (obj == null) {
                // We did not resolve object according to its param name, let's
                // check with potential alias
                String[] aliases = entry.getValue().getAnnotation(Param.class).alias();
                if (aliases != null) {
                    for (String alias : entry.getValue().getAnnotation(Param.class).alias()) {
                        obj = resolveObject(ctx, alias, args);
                        if (obj != null) {
                            break;
                        }
                    }
                }
            }
            if (obj == null) {
                if (entry.getValue().getAnnotation(Param.class).required()) {
                    throw new OperationException("Failed to inject parameter '" + entry.getKey()
                            + "'. Seems it is missing from the context. Operation: " + getId());
                } // else do nothing
            } else {
                Field field = entry.getValue();
                Class<?> cl = obj.getClass();
                if (!field.getType().isAssignableFrom(cl)) {
                    // try to adapt
                    obj = service.getAdaptedValue(ctx, obj, field.getType());
                }
                try {
                    field.set(target, obj);
                } catch (ReflectiveOperationException e) {
                    throw new OperationException(e);
                }
            }
        }
        for (Field field : injectableFields) {
            Object obj = ctx.getAdapter(field.getType());
            try {
                field.set(target, obj);
            } catch (ReflectiveOperationException e) {
                throw new OperationException(e);
            }
        }
    }

    @Override
    public InvokableMethod[] getMethodsMatchingInput(Class<?> in) {
        List<Match> result = new ArrayList<Match>();
        for (InvokableMethod m : methods) {
            int priority = m.inputMatch(in);
            if (priority > 0) {
                result.add(new Match(m, priority));
            }
        }
        int size = result.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return new InvokableMethod[] { result.get(0).method };
        }
        Collections.sort(result);
        InvokableMethod[] ar = new InvokableMethod[result.size()];
        for (int i = 0; i < ar.length; i++) {
            ar[i] = result.get(i).method;
        }
        return ar;
    }

    @Override
    public OperationDocumentation getDocumentation() {
        Operation op = type.getAnnotation(Operation.class);
        OperationDocumentation doc = new OperationDocumentation(op.id());
        doc.label = op.label();
        doc.requires = op.requires();
        doc.category = op.category();
        doc.since = op.since();
        doc.deprecatedSince = op.deprecatedSince();
        doc.addToStudio = op.addToStudio();
        doc.setAliases(op.aliases());
        doc.implementationClass = type.getName();
        if (doc.requires.length() == 0) {
            doc.requires = null;
        }
        if (doc.label.length() == 0) {
            doc.label = doc.id;
        }
        doc.description = op.description();
        // load parameters information
        List<OperationDocumentation.Param> paramsAccumulator = new LinkedList<OperationDocumentation.Param>();
        for (Field field : params.values()) {
            Param p = field.getAnnotation(Param.class);
            OperationDocumentation.Param param = new OperationDocumentation.Param();
            param.name = p.name();
            param.description = p.description();
            param.type = getParamDocumentationType(field.getType());
            param.widget = p.widget();
            if (param.widget.length() == 0) {
                param.widget = null;
            }
            param.order = p.order();
            param.values = p.values();
            param.required = p.required();
            paramsAccumulator.add(param);
        }
        Collections.sort(paramsAccumulator);
        doc.params = paramsAccumulator.toArray(new OperationDocumentation.Param[paramsAccumulator.size()]);
        // load signature
        ArrayList<String> result = new ArrayList<String>(methods.size() * 2);
        Collection<String> collectedSigs = new HashSet<String>();
        for (InvokableMethod m : methods) {
            String in = getParamDocumentationType(m.getInputType(), m.isIterable());
            String out = getParamDocumentationType(m.getOutputType());
            String sigKey = in + ":" + out;
            if (!collectedSigs.contains(sigKey)) {
                result.add(in);
                result.add(out);
                collectedSigs.add(sigKey);
            }
        }
        doc.signature = result.toArray(new String[result.size()]);
        // widgets descriptor
        if (widgetDefinitionList != null) {
            doc.widgetDefinitions = widgetDefinitionList.toArray(new WidgetDefinition[widgetDefinitionList.size()]);
        }
        return doc;
    }

    @Override
    public String getContributingComponent() {
        return contributingComponent;
    }

    protected String getParamDocumentationType(Class<?> type) {
        return getParamDocumentationType(type, false);
    }

    protected String getParamDocumentationType(Class<?> type, boolean isIterable) {
        String t;
        if (DocumentModel.class.isAssignableFrom(type) || DocumentRef.class.isAssignableFrom(type)) {
            t = isIterable ? Constants.T_DOCUMENTS : Constants.T_DOCUMENT;
        } else if (DocumentModelList.class.isAssignableFrom(type) || DocumentRefList.class.isAssignableFrom(type)) {
            t = Constants.T_DOCUMENTS;
        } else if (BlobList.class.isAssignableFrom(type)) {
            t = Constants.T_BLOBS;
        } else if (Blob.class.isAssignableFrom(type)) {
            t = isIterable ? Constants.T_BLOBS : Constants.T_BLOB;
        } else if (URL.class.isAssignableFrom(type)) {
            t = Constants.T_RESOURCE;
        } else if (Calendar.class.isAssignableFrom(type)) {
            t = Constants.T_DATE;
        } else {
            t = type.getSimpleName().toLowerCase();
        }
        return t;
    }

    @Override
    public String toString() {
        return "OperationTypeImpl [id=" + id + ", type=" + type + ", params=" + params + "]";
    }

    /**
     * @since 5.7.2
     */
    @Override
    public List<InvokableMethod> getMethods() {
        return methods;
    }
}
