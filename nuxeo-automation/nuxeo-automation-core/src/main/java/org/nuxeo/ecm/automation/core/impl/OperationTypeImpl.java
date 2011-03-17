/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationTypeImpl implements OperationType {

    /**
     * The service that registered the operation
     */
    protected AutomationService service;

    /**
     * The operation ID - used for lookups.
     */
    protected String id;

    /**
     * The operation type
     */
    protected Class<?> type;

    /**
     * Injectable parameters. a map between the parameter name and the Field
     * object
     */
    protected Map<String, Field> params;

    /**
     * Invocable methods
     */
    protected List<InvokableMethod> methods;

    /**
     * Fields that should be injected from context
     */
    protected List<Field> injectableFields;


    public OperationTypeImpl(AutomationService service, Class<?> type) {
        Operation anno = type.getAnnotation(Operation.class);
        if (anno == null) {
            throw new IllegalArgumentException("Invalid operation class: "
                    + type + ". No @Operation annotation found on class.");
        }
        this.service = service;
        this.type = type;
        id = anno.id();
        if (id.length() == 0) {
            id = type.getName();
        }
        params = new HashMap<String, Field>();
        methods = new ArrayList<InvokableMethod>();
        injectableFields = new ArrayList<Field>();
        initMethods();
        initFields();
    }

    public AutomationService getService() {
        return service;
    }

    public String getId() {
        return id;
    }

    public Class<?> getType() {
        return type;
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

    public Object newInstance(OperationContext ctx, Map<String, Object> args)
            throws Exception {
        Object obj = type.newInstance();
        inject(ctx, args, obj);
        return obj;
    }

    public void inject(OperationContext ctx, Map<String, Object> args,
            Object target) throws Exception {
        for (Map.Entry<String, Field> entry : params.entrySet()) {
            Object obj = args.get(entry.getKey());
            if (obj instanceof Expression) {
                obj = ((Expression) obj).eval(ctx);
            }
            if (obj == null) {
                if (entry.getValue().getAnnotation(Param.class).required()) {
                    throw new OperationException(
                            "Failed to inject parameter '"
                                    + entry.getKey()
                                    + "'. Seems it is missing from the context. Operation: "
                                    + getId());
                } // else do nothing
            } else {
                Field field = entry.getValue();
                Class<?> cl = obj.getClass();
                if (!field.getType().isAssignableFrom(cl)) {
                    // try to adapt
                    obj = service.getAdaptedValue(ctx, obj, field.getType());
                }
                field.set(target, obj);
            }
        }
        for (Field field : injectableFields) {
            Object obj = ctx.getAdapter(field.getType());
            field.set(target, obj);
        }
    }

    public List<InvokableMethod> getMethods() {
        return methods;
    }

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

    static class Match implements Comparable<Match> {
        protected InvokableMethod method;

        int priority;

        Match(InvokableMethod method, int priority) {
            this.method = method;
            this.priority = priority;
        }

        public int compareTo(Match o) {
            return o.priority - priority;
        }
    }

    public OperationDocumentation getDocumentation() {
        Operation op = type.getAnnotation(Operation.class);
        OperationDocumentation doc = new OperationDocumentation(op.id());
        doc.label = op.label();
        doc.requires = op.requires();
        doc.category = op.category();
        doc.since = op.since();
        if (doc.requires.length() == 0) {
            doc.requires = null;
        }
        if (doc.label.length() == 0) {
            doc.label = doc.id;
        }
        doc.description = op.description();
        // load parameters information
        doc.params = new ArrayList<OperationDocumentation.Param>();
        for (Field field : params.values()) {
            Param p = field.getAnnotation(Param.class);
            OperationDocumentation.Param param = new OperationDocumentation.Param();
            param.name = p.name();
            param.type = getParamDocumentationType(field.getType());
            param.widget = p.widget();
            if (param.widget.length() == 0) {
                param.widget = null;
            }
            param.order = p.order();
            param.values = p.values();
            param.isRequired = p.required();
            doc.params.add(param);
        }
        Collections.sort(doc.params);
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
        return doc;
    }

    protected String getParamDocumentationType(Class<?> type) {
        return getParamDocumentationType(type, false);
    }

    protected String getParamDocumentationType(Class<?> type, boolean isIterable) {
        String t;
        if (DocumentModel.class.isAssignableFrom(type)
                || DocumentRef.class.isAssignableFrom(type)) {
            t = isIterable ? Constants.T_DOCUMENTS : Constants.T_DOCUMENT;
        } else if (DocumentModelList.class.isAssignableFrom(type)
                || DocumentRefList.class.isAssignableFrom(type)) {
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
}
