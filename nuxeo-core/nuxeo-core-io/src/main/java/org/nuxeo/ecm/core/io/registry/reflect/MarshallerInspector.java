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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.registry.Marshaller;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContextImpl;
import org.nuxeo.ecm.core.io.registry.context.ThreadSafeRenderingContext;
import org.nuxeo.runtime.api.Framework;

import javax.inject.Inject;

/**
 * Utility class used to instanciate marshallers. This class checks if a marshaller has annotation {@link Setup} and
 * inspect every attributes having {@link Inject} annotation.
 * <p>
 * To get a valid marshaller instance :
 * <ul>
 * <li>Create an inspector for your marshaller class using constructor {@link #MarshallerInspector(Class)}. This will
 * checks your marshaller has annotation {@link Setup} and inspect every attributes having {@link Inject} annotation.</li>
 * <li>You can check it's a valid marshaller by calling @ #isValid()}</li>
 * <li>You can check it's a {@link Writer} by calling {@link #isWriter()}</li>
 * <li>You can check it's a {@link Reader} by calling {@link #isReader()}</li>
 * <li>You can finally call {@link #getInstance(RenderingContext)} to get a valid marshaller instance with
 * {@link RenderingContext} and required services injected.</li>
 * </ul>
 * </p>
 * <p>
 * This class implements {@link Comparable} and then handle marshaller priorities rules: look at
 * {@link MarshallerRegistry} javadoc to read the rules.
 * </p>
 *
 * @since 7.2
 */
public class MarshallerInspector implements Comparable<MarshallerInspector> {

    private static final Log log = LogFactory.getLog(MarshallerInspector.class);

    private Class<?> clazz;

    private Integer priority;

    private Instantiations instantiation;

    private List<MediaType> supports = new ArrayList<MediaType>();

    private Constructor<?> constructor;

    private List<Field> serviceFields = new ArrayList<Field>();

    private List<Field> contextFields = new ArrayList<Field>();

    private Object singleton;

    private ThreadLocal<Object> threadInstance;

    private Class<?> marshalledType;

    private Type genericType;

    /**
     * Create an inspector for the given class.
     *
     * @param clazz The class to analyse and instantiate.
     */
    public MarshallerInspector(Class<?> clazz) {
        this.clazz = clazz;
        load();
    }

    /**
     * Introspect this marshaller: gets instantiation mode, supported mimetype, gets the managed class, generic type and
     * load every needed injection to be ready to create an instance quickly.
     *
     * @since 7.2
     */
    private void load() {
        // checks if there's a public constructor without parameters
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (Modifier.isPublic(constructor.getModifiers()) && constructor.getParameterTypes().length == 0) {
                this.constructor = constructor;
                break;
            }
        }
        if (constructor == null) {
            throw new MarshallingException("No public constructor found for class " + clazz.getName()
                    + ". Instanciation will not be possible.");
        }
        // load instantiation mode
        Setup setup = loadSetup(clazz);
        if (setup == null) {
            throw new MarshallingException("No required @Setup annotation found for class " + clazz.getName()
                    + ". Instanciation will not be possible.");
        }
        if (!isReader() && !isWriter()) {
            throw new MarshallingException(
                    "MarshallerInspector only supports Reader and Writer: you must implement one of this interface for this class: "
                            + clazz.getName());
        }
        if (isReader() && isWriter()) {
            throw new MarshallingException(
                    "MarshallerInspector only supports either Reader or Writer: you must implement only one of this interface: "
                            + clazz.getName());
        }
        instantiation = setup.mode();
        priority = setup.priority();
        // load supported mimetype
        Supports supports = loadSupports(clazz);
        if (supports != null) {
            for (String mimetype : supports.value()) {
                try {
                    MediaType mediaType = MediaType.valueOf(mimetype);
                    this.supports.add(mediaType);
                } catch (IllegalArgumentException e) {
                    log.warn("In marshaller class " + clazz.getName() + ", the declared mediatype " + mimetype
                            + " cannot be parsed as a mimetype");
                }
            }
        }
        if (this.supports.isEmpty()) {
            log.warn("The marshaller " + clazz.getName()
                    + " does not support any mimetype. You can add some using annotation @Supports");
        }
        // loads the marshalled type and generic type
        loadMarshalledType(clazz);
        // load properties that require injection
        loadInjections(clazz);
        // warn if several context found
        if (contextFields.size() > 1) {
            log.warn("The marshaller "
                    + clazz.getName()
                    + " has more than one context injected property. You probably should use a context from a parent class.");
        }
    }

    /**
     * Get the Java class and generic type managed by this marshaller. If not found, search in the parent.
     *
     * @param clazz The marshaller class to analyse.
     * @since 7.2
     */
    private void loadMarshalledType(Class<?> clazz) {
        if (isWriter() || isReader()) {
            Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(clazz, Marshaller.class);
            for (Map.Entry<TypeVariable<?>, Type> entry : typeArguments.entrySet()) {
                if (Marshaller.class.equals(entry.getKey().getGenericDeclaration())) {
                    genericType = TypeUtils.unrollVariables(typeArguments, entry.getValue());
                    marshalledType = TypeUtils.getRawType(genericType, null);
                    break;
                }
            }
        }
    }

    /**
     * Get the first found {@link Setup} annotation in the class hierarchy. If not found in the given class, search in
     * the parent.
     *
     * @param clazz The class to analyse.
     * @return The first found {@link Setup} annotation.
     * @since 7.2
     */
    private Setup loadSetup(Class<?> clazz) {
        if (Object.class.equals(clazz)) {
            return null;
        }
        return clazz.getAnnotation(Setup.class);
    }

    /**
     * Get the first found {@link Supports} annotation in the class hierarchy. If not found in the given class, search
     * in the parent.
     *
     * @param clazz The class to analyse.
     * @return The first found {@link Supports} annotation.
     * @since 7.2
     */
    private Supports loadSupports(Class<?> clazz) {
        if (Object.class.equals(clazz)) {
            return null;
        }
        Supports supports = clazz.getAnnotation(Supports.class);
        if (supports != null) {
            return supports;
        } else {
            return loadSupports(clazz.getSuperclass());
        }
    }

    /**
     * Load every properties that require injection (context and Nuxeo service). Search in the given class and recurse
     * in the parent class.
     *
     * @param clazz The class to analyse.
     * @since 7.2
     */
    private void loadInjections(Class<?> clazz) {
        if (Object.class.equals(clazz)) {
            return;
        }
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                if (RenderingContext.class.equals(field.getType())) {
                    field.setAccessible(true);
                    contextFields.add(field);
                } else {
                    field.setAccessible(true);
                    serviceFields.add(field);
                }
            }
        }
        loadInjections(clazz.getSuperclass());
    }

    /**
     * Create an instance of this marshaller. Depending on the instantiation mode, get the current singleton instance,
     * get a thread local one or create a new one.
     *
     * @param ctx The {@link RenderingContext} to inject, if null create an empty context.
     * @return An instance of this class.
     * @since 7.2
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(RenderingContext ctx) {
        RenderingContext realCtx = getRealContext(ctx);
        switch (instantiation) {
        case SINGLETON:
            return (T) getSingletonInstance(realCtx);
        case PER_THREAD:
            return (T) getThreadInstance(realCtx);
        case EACH_TIME:
            return (T) getNewInstance(realCtx, false);
        default:
            return null;
        }
    }

    /**
     * Get the real context implementation from the given one. If it's a {@link ThreadSafeRenderingContext}, gets the
     * enclosing one. If the given context is null, create an empty context.
     *
     * @param ctx The {@link RenderingContext} from which we want to search for a real context.
     * @return A {@link RenderingContextImpl}.
     * @since 7.2
     */
    private RenderingContext getRealContext(RenderingContext ctx) {
        if (ctx == null) {
            return RenderingContext.CtxBuilder.get();
        }
        if (ctx instanceof RenderingContextImpl) {
            return ctx;
        }
        if (ctx instanceof ThreadSafeRenderingContext) {
            RenderingContext delegate = ((ThreadSafeRenderingContext) ctx).getDelegate();
            return getRealContext(delegate);
        }
        return null;
    }

    /**
     * Create or get a singleton instance of the marshaller.
     *
     * @param ctx The {@link RenderingContext} to inject.
     * @return An instance of the marshaller.
     * @since 7.2
     */
    private Object getSingletonInstance(RenderingContext ctx) {
        if (singleton == null) {
            singleton = getNewInstance(ctx, true);
        } else {
            for (Field contextField : contextFields) {
                ThreadSafeRenderingContext value;
                try {
                    value = (ThreadSafeRenderingContext) contextField.get(singleton);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    log.error("unable to create a marshaller instance for clazz " + clazz.getName(), e);
                    return null;
                }
                value.configureThread(ctx);
            }
        }
        return singleton;
    }

    /**
     * Create or get a thread local instance of the marshaller.
     *
     * @param ctx The {@link RenderingContext} to inject.
     * @return An instance of the marshaller.
     * @since 7.2
     */
    private Object getThreadInstance(RenderingContext ctx) {
        if (threadInstance == null) {
            threadInstance = new ThreadLocal<Object>();
        }
        Object instance = threadInstance.get();
        if (instance == null) {
            instance = getNewInstance(ctx, false);
            threadInstance.set(instance);
        } else {
            for (Field contextField : contextFields) {
                try {
                    contextField.set(instance, ctx);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    log.error("unable to create a marshaller instance for clazz " + clazz.getName(), e);
                    return null;
                }
            }
        }
        return instance;
    }

    /**
     * Create a new instance of the marshaller.
     *
     * @param ctx The {@link RenderingContext} to inject.
     * @return An instance of the marshaller.
     * @since 7.2
     */
    public Object getNewInstance(RenderingContext ctx, boolean threadSafe) {
        try {
            Object instance = clazz.newInstance();
            for (Field contextField : contextFields) {
                if (threadSafe) {
                    ThreadSafeRenderingContext safeCtx = new ThreadSafeRenderingContext();
                    safeCtx.configureThread(ctx);
                    contextField.set(instance, safeCtx);
                } else {
                    contextField.set(instance, ctx);
                }
            }
            for (Field serviceField : serviceFields) {
                Object service = Framework.getService(serviceField.getType());
                if (service == null) {
                    log.error("unable to inject a service " + serviceField.getType().getName()
                            + " in the marshaller clazz " + clazz.getName());
                    return null;
                }
                serviceField.set(instance, service);
            }
            return instance;
        } catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
            log.error("unable to create a marshaller instance for clazz " + clazz.getName(), e);
            return null;
        }
    }

    public Instantiations getInstantiations() {
        return instantiation;
    }

    public Integer getPriority() {
        return priority;
    }

    public List<MediaType> getSupports() {
        return supports;
    }

    public Class<?> getMarshalledType() {
        return marshalledType;
    }

    public Type getGenericType() {
        return genericType;
    }

    public boolean isMarshaller() {
        return Marshaller.class.isAssignableFrom(clazz);
    }

    public boolean isWriter() {
        return Writer.class.isAssignableFrom(clazz);
    }

    public boolean isReader() {
        return Reader.class.isAssignableFrom(clazz);
    }

    @Override
    public int compareTo(MarshallerInspector inspector) {
        if (inspector != null) {
            // compare priorities
            int result = getPriority().compareTo(inspector.getPriority());
            if (result != 0) {
                return -result;
            }
            // then, compare instantiation mode: singleton > thread > each
            result = getInstantiations().compareTo(inspector.getInstantiations());
            if (result != 0) {
                return -result;
            }
            // specialize marshaller are preferred: managed class IntegerProperty > AbstractProperty > Property
            if (isMarshaller() && inspector.isMarshaller()) {
                if (!getMarshalledType().equals(inspector.getMarshalledType())) {
                    if (getMarshalledType().isAssignableFrom(inspector.getMarshalledType())) {
                        return 1;
                    } else if (inspector.getMarshalledType().isAssignableFrom(getMarshalledType())) {
                        return -1;
                    }
                }
            }
            // force sub classes to manage their priorities: StandardWriter > CustomWriter extends StandardWriter
            // let the reference implementations priority
            if (!clazz.equals(inspector.clazz)) {
                if (clazz.isAssignableFrom(inspector.clazz)) {
                    return -1;
                } else if (inspector.clazz.isAssignableFrom(clazz)) {
                    return 1;
                }
            }
            // This is just optimization :
            // priorise DocumentModel, Property
            // then NuxeoPrincipal, NuxeoGroup and List<DocumentModel>
            if ((isWriter() && inspector.isWriter()) || (isReader() && inspector.isReader())) {
                boolean mineIsTop = isTopPriority(genericType);
                boolean thatIsTop = isTopPriority(inspector.genericType);
                if (mineIsTop && !thatIsTop) {
                    return -1;
                } else if (!mineIsTop && thatIsTop) {
                    return 1;
                }
                boolean mineIsBig = isBigPriority(genericType);
                boolean thatIsBig = isBigPriority(inspector.genericType);
                if (mineIsBig && !thatIsBig) {
                    return -1;
                } else if (!mineIsBig && thatIsBig) {
                    return 1;
                }
            }
            return -clazz.getName().compareTo(inspector.clazz.getName());
        }
        return 1;
    }

    private static boolean isTopPriority(Type type) {
        return TypeUtils.isAssignable(type, DocumentModel.class) || TypeUtils.isAssignable(type, Property.class);
    }

    private static boolean isBigPriority(Type type) {
        return TypeUtils.isAssignable(type, NuxeoPrincipal.class) || TypeUtils.isAssignable(type, NuxeoGroup.class)
                || TypeUtils.isAssignable(type, TypeUtils.parameterize(List.class, DocumentModel.class));
    }

    /**
     * Two {@link MarshallerInspector} are equals if their managed clazz are the same.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MarshallerInspector)) {
            return false;
        }
        MarshallerInspector inspector = (MarshallerInspector) obj;
        if (clazz != null) {
            return clazz.equals(inspector.clazz);
        } else {
            if (inspector.clazz == null) {
                return true;
            }
        }
        return false;
    }

}
