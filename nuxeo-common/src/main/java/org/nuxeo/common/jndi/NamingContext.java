/*
 *    Copyright 2004 Original mockejb authors.
 *    Copyright 2007 Nuxeo SAS.
 *
 * This file is derived from mockejb-0.6-beta2
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
 */
package org.nuxeo.common.jndi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.NotContextException;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;

/**
 * Provides implementation of <code>javax.naming.Context</code> interface for
 * hierarchical in memory single-namespace naming system.
 * A name in the <code>NamingContext</code> namespace is a sequence of one or more
 * atomic names, relative to a root initial context.
 * When a name consist of more than one atomic names it is a <code>CompoundName</code>
 * where atomic names are separated with separator character - '/' or '.'.
 * It is possible to use both separator characters in the same name. In such cases
 * any occurrences of '.' are replaced with '/' before parsing.
 * <p>
 * Leading and terminal components of a <code>CompoundName</code> can not be empty -
 * for example "name1/name2/name3" is a valid name, while the following names are
 * not valid - "/name1/name2/name3", "name1/name2/name3/", "/name1/name2/name3/".
 * If such name is passed, all empty leading/terminal components will be removed
 * before the name is actually used (this will not affect the original value) -
 * from the above three examples the actual name will be "name1/name2/name3".
 * If a name contains intermediate empty components (for example "a//b") then
 * <code>InvalidNameException</code> will be thrown.
 * <p>
 * Composite names (instances of <code>CompositeName</code>) must contain zero or one
 * component from the <code>NamingContext</code> namespace.
 * <p>
 * The namespace of <code>NamingContext</code> can be represented as a tree of atomic names.
 * Each name is bound to an instance of NamingContext (subcontext) or to an arbitrary object.
 * Each subcontext has collection of names bound to other subcontexts or arbitrary objects.
 * <p>
 * When instance of <code>Name</code> is used as parameter to any of the
 * NamingContext methods, if the object is not <code>CompositeName</code> then
 * it is assumed that it is <code>CompoundName</code>
 * <p>
 * Example:
 * <pre><code>
 * myContext = initialContext.lookup("foo");
 * myObject = myContext.lookup("bar");
 * </code>
 * is equivalent to
 * <code>myObject = initialContext.lookup("foo/bar");</code>
 * </pre>
 * <p>
 * Instances of <code>NamingContext</code> are created only through
 * <code>NamingContextFactory</code>, when <code>InitialContext</code> is instantiated.
 * <p>
 * If a remote context is provided, this class will search in that remote context if the
 * object is not found locally.
 * <p>
 * For overloaded methods that accept name as <code>String</code> or
 * <code>Name</code> only the version for <code>Name</code> is documented.
 * The <code>String</code> version creates <code>CompoundName</code>, from
 * the string name passed as parameter, and calls the <code>Name</code> version of
 * the same method.
 *
 * @author Alexander Ananiev
 * @author Dimitar Gospodinov
 */
public class NamingContext implements Context {

    private static final String ROOT_CONTEXT_NAME = "ROOT";

    // NamingContext supports single naming scheme and all instances use the same parser.
    private static final NameParser nameParser = new NamingContextNameParser();

    /**
     * NamingContext name parser.
     *
     * @author Dimitar Gospodinov
     */
    public static class NamingContextNameParser implements NameParser {

        private static final Properties syntax = new Properties();
        static {
            syntax.put("jndi.syntax.direction", "left_to_right");
            syntax.put("jndi.syntax.separator", "/");
            syntax.put("jndi.syntax.ignorecase", "false");
            syntax.put("jndi.syntax.trimblanks", "yes");
        }

        /**
         * Parses <code>name</code> into <code>CompoundName</code>
         * using the following <code>CompoundName</code> properties:
         * <pre>
         * jndi.syntax.direction = "left_to_right"
         * jndi.syntax.separator = "/"
         * jndi.syntax.ignorecase = "false"
         * jndi.syntax.trimblanks = "yes"
         * </pre>
         * Any characters '.' in the name <code>name</code> will be replaced with the
         * separator character specified above, before parsing.
         *
         * @param name name to parse
         * @throws NamingException if a naming error occurs
         */
        public Name parse(String name) throws NamingException {
            return new CompoundName(name.replace('.', '/'), syntax);
        }
    }

    /**
     * Map of objects registered for this context representing the local
     * context.
     */
    private final Map<Object, Object> objects = Collections.synchronizedMap(new HashMap<Object, Object>());

    /** Parent Context of this Context. */
    private final NamingContext parent;

    /** Atomic name of this Context. */
    private final String contextName;

    /**
     * Container context used for delegated lookups.
     */
    private final Context remoteContext;

    // Shows if this context has been destroyed
    private boolean isDestroyed;

    /**
     * Creates a new instance of the context. This class can only be
     * instantiated by its factory.
     *
     * @param remoteContext remote context that NamingContext will delegate to if
     *            it fails to lookup an object locally
     */
    protected NamingContext(Context remoteContext) {
        this(remoteContext, null, ROOT_CONTEXT_NAME);
    }

    /**
     * Creates new instance of <code>NamingContext</code>.
     *
     * @param remoteContext remote context that NamingContext will delegate to if
     *            it fails to lookup an object locally
     * @param parent parent context of this context. <code>null</code> if this
     *            is the root context.
     * @param name atomic name for this context
     */
    private NamingContext(Context remoteContext, NamingContext parent, String name) {
        this.remoteContext = remoteContext;
        this.parent = parent;
        contextName = name;
        isDestroyed = false;
    }

    /**
     * Not implemented.
     *
     * @see javax.naming.Context#addToEnvironment(java.lang.String, java.lang.Object)
     */
    public Object addToEnvironment(String arg0, Object arg1)
        throws NamingException {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Binds object <code>obj</code> to name <code>name</code> in this
     * context. Intermediate contexts that do not exist will be created.
     *
     * @param name name of the object to bind
     * @param obj object to bind. Can be <code>null</code>.
     *
     * @throws NoPermissionException if this context has been destroyed
     * @throws InvalidNameException if <code>name</code> is empty or is
     *             <code>CompositeName</code> that spans more than one naming
     *             system
     * @throws NotContextException if <code>name</code> has more than one
     *             atomic name and intermediate atomic name is bound to object
     *             that is not context.
     *
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind(Name name, Object obj) throws NamingException {
        checkIsDestroyed();
        // Do not check for already bound name. Simply replace the existing value.
        rebind(name, obj);
    }

    /**
     * Binds object <code>obj</code> to name <code>name</code> in this
     * context.
     *
     * @param name name of the object to add
     * @param obj object to bind
     * @throws NamingException if naming error occurs
     * @see #bind(Name, Object)
     */
    public void bind(String name, Object obj) throws NamingException {
        bind(nameParser.parse(name), obj);
    }

    /**
     * Does nothing.
     *
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException {
    }

    /**
     * Returns composition of <code>prefix</code> and <code>name</code>.
     *
     * @param name name relative to this context
     * @param prefix name of this context
     * @see javax.naming.Context#composeName(javax.naming.Name,
     *      javax.naming.Name)
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        checkIsDestroyed();
        /*
         * We do not want to modify any of the parameters (JNDI requirement).
         * Clone <code>prefix</code> to satisfy the requirement.
         */
        Name parsedPrefix = getParsedName((Name) prefix.clone());
        Name parsedName = getParsedName(name);

        return parsedPrefix.addAll(parsedName);
    }

    /**
     * Composes the name of this context with a name relative to this context.
     * Given a name (name) relative to this context, and the name (prefix)
     * of this context relative to one of its ancestors, this method returns
     * the composition of the two names using the syntax appropriate for
     * the naming system(s) involved.
     * <p>
     * Example:
     * <pre>
     * composeName("a","b")  b/a
     * composeName("a","")  a
     * </pre>
     *
     * @param name name relative to this context
     * @param prefix name of this context
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName(String name, String prefix)
        throws NamingException {

        checkIsDestroyed();
        return composeName(nameParser.parse(name), nameParser.parse(prefix)).toString();
    }

    /**
     * Creates subcontext with name <code>name</code>, relative to this
     * Context.
     *
     * @param name subcontext name.
     * @return new subcontext named <code>name</code> relative to this context
     * @throws NoPermissionException if this context has been destroyed
     * @throws InvalidNameException if <code>name</code> is empty or is
     *             <code>CompositeName</code> that spans more than one naming
     *             system
     * @throws NameAlreadyBoundException if <code>name</code> is already bound
     *             in this Context
     * @throws NotContextException if any intermediate name from
     *             <code>name</code> is not bound to instance of
     *             <code>javax.naming.Context</code>
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext(Name name)
        throws NamingException {

        checkIsDestroyed();
        Name parsedName = getParsedName(name);
        if (parsedName.isEmpty() || parsedName.get(0).length() == 0) {
            throw new InvalidNameException("Name can not be empty!");
        }
        String subContextName = parsedName.get(0);
        Object boundObject = objects.get(parsedName.get(0));

        if (parsedName.size() == 1) {
            // Check if <code>name</code> is already in use
            if (boundObject == null) {
                Context subContext =
                    new NamingContext(remoteContext, this, subContextName);
                objects.put(subContextName, subContext);
                return subContext;
            } else {
                throw new NameAlreadyBoundException(
                    "Name " + subContextName + " is already bound!");
            }
        } else {
            if (boundObject instanceof Context) {
                // Let the subcontext create new subcontext
                return ((Context) boundObject).createSubcontext(
                    parsedName.getSuffix(1));
            } else {
                throw new NotContextException(
                    "Expected Context but found " + boundObject);
            }
        }
    }

    /**
     * Creates subcontext with name <code>name</code>, relative to this
     * Context.
     *
     * @param name subcontext name
     * @return new subcontext named <code>name</code> relative to this context
     * @throws NamingException if naming error occurs
     * @see #createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(nameParser.parse(name));
    }

    /**
     * Destroys subcontext with name <code>name</code>. The subcontext must
     * be empty, otherwise <code>ContextNotEmptyException</code> is thrown.
     * <p>
     * Once a context is destroyed, the instance should not be used.
     *
     * @param name subcontext to destroy
     * @throws NoPermissionException if this context has been destroyed
     * @throws InvalidNameException if <code>name</code> is empty or is
     *             <code>CompositeName</code> that spans more than one naming
     *             system
     * @throws ContextNotEmptyException if Context <code>name</code> is not
     *             empty
     * @throws NameNotFoundException if subcontext with name <code>name</code>
     *             can not be found
     * @throws NotContextException if <code>name</code> is not bound to
     *             instance of <code>NamingContext</code>
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext(Name name) throws NamingException {
        checkIsDestroyed();
        Name parsedName = getParsedName(name);
        if (parsedName.isEmpty() || parsedName.get(0).length() == 0) {
            throw new InvalidNameException("Name can not be empty!");
        }
        String subContextName = parsedName.get(0);
        Object boundObject = objects.get(subContextName);
        if (boundObject == null) {
            throw new NameNotFoundException(
                "Name " + subContextName + "not found in the context!");
        }
        if (!(boundObject instanceof NamingContext)) {
            throw new NotContextException();
        }
        NamingContext contextToDestroy = (NamingContext) boundObject;
        if (parsedName.size() == 1) {
            /*
             * Check if the Context to be destroyed is empty.
             * Can not destroy non-empty Context.
             */
            if (contextToDestroy.objects.isEmpty()) {
                objects.remove(subContextName);
                contextToDestroy.destroyInternal();
            } else {
                throw new ContextNotEmptyException("Can not destroy non-empty Context!");
            }
        } else {
            // Let the subcontext destroy the context
            ((Context) boundObject).destroySubcontext(
                parsedName.getSuffix(1));
        }
    }

    /**
     * Destroys subcontext with name <code>name</code>.
     *
     * @param name name of subcontext to destroy
     * @throws NamingException if naming error occurs
     * @see #destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(nameParser.parse(name));
    }

    /**
     * Not implemented.
     *
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Not implemented.
     *
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Retrieves name parser used to parse context with name <code>name</code>.
     *
     * @param name context name
     * @return <code>NameParser</code>
     * @throws NoPermissionException if this context has been destroyed
     * @throws NamingException if any other naming error occurs
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser(Name name) throws NamingException {
        checkIsDestroyed();
        return nameParser;
    }

    /**
     * Retrieves name parser used to parse context with name <code>name</code>.
     *
     * @param name context name
     * @return <code>NameParser</code>
     * @throws NamingException if naming error occurs
     * @see #getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser(String name) throws NamingException {
        checkIsDestroyed();
        return nameParser;
    }

    /**
     * The same as <code>listBindings(String)</code>.
     *
     * @param name name of Context, relative to this Context
     * @return <code>NamingEnumeration</code> of all name-class pairs. Each
     *         element from the enumeration is instance of
     *         <code>NameClassPair</code>
     * @throws NamingException if naming error occurs
     * @see #listBindings(javax.naming.Name)
     */
    @SuppressWarnings("unchecked")
    public NamingEnumeration list(Name name) throws NamingException {
        return listBindings(name);
    }

    /**
     * The same as <code>listBindings(String)</code>.
     *
     * @param name name of Context, relative to this Context
     * @return <code>NamingEnumeration</code> of all name-class pairs. Each
     *         element from the enumeration is instance of
     *         <code>NameClassPair</code>
     * @throws NamingException if naming error occurs
     * @see #listBindings(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public NamingEnumeration list(String name) throws NamingException {
        return list(nameParser.parse(name));
    }

    /**
     * Lists all bindings for Context with name <code>name</code>. If
     * <code>name</code> is empty, then this Context is assumed.
     *
     * @param name name of Context, relative to this Context
     * @return <code>NamingEnumeration</code> of all name-object pairs. Each
     *         element from the enumeration is instance of <code>Binding</code>
     * @throws NoPermissionException if this context has been destroyed
     * @throws InvalidNameException if <code>name</code> is
     *             <code>CompositeName</code> that spans more than one naming
     *             system
     * @throws NameNotFoundException if <code>name</code> can not be found
     * @throws NotContextException component of <code>name</code> is not bound
     *             to instance of <code>NamingContext</code>, when
     *             <code>name</code> is not an atomic name
     * @throws NamingException if any other naming error occurs
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        checkIsDestroyed();
        Name parsedName = getParsedName(name);
        if (parsedName.isEmpty()) {
            Vector<Binding> bindings = new Vector<Binding>();
            for (Object o : objects.keySet()) {
                String bindingName = (String) o;
                bindings.addElement(
                        new Binding(bindingName, objects.get(bindingName)));
            }
            return new NamingEnumerationImpl(bindings);
        } else {
            Object subContext = objects.get(parsedName.get(0));
            if (subContext instanceof Context) {
                return ((Context) subContext).listBindings(parsedName.getSuffix(1));
            }
            if (subContext == null
                && !objects.containsKey(parsedName.get(0))) {
                throw new NameNotFoundException("Name " + name + " not found");
            } else {
                throw new NotContextException(
                    "Expected Context but found " + subContext);
            }
        }
    }

    /**
     * Lists all bindings for Context with name <code>name</code>. If
     * <code>name</code> is empty then this Context is assumed.
     *
     * @param name name of Context, relative to this Context
     * @return <code>NamingEnumeration</code> of all name-object pairs. Each
     *         element from the enumeration is instance of <code>Binding</code>
     * @throws NamingException if naming error occurs
     * @see #listBindings(javax.naming.Name)
     */
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return listBindings(nameParser.parse(name));
    }

    /**
     * Looks up object with name <code>name</code> in this context. If the
     * object is not found and the remote context was provided, calls the remote
     * context to lookup the object.
     *
     * @param name name to look up
     * @return object reference bound to name <code>name</code>
     * @throws NoPermissionException if this context has been destroyed
     * @throws InvalidNameException if <code>name</code> is
     *             <code>CompositeName</code> that spans more than one naming
     *             system
     * @throws NameNotFoundException if <code>name</code> can not be found
     * @throws NotContextException component of <code>name</code> is not bound
     *             to instance of <code>NamingContext</code>, when
     *             <code>name</code> is not atomic name.
     * @throws NamingException if any other naming error occurs
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup(Name name) throws NamingException {
        checkIsDestroyed();
        try {
            return lookupInternal(name);
        } catch (NameNotFoundException ex) {
            // Shall we delegate?
            if (remoteContext != null) {
                return remoteContext.lookup(name);
            } else {
                throw new NameNotFoundException("Name " + name + " not found. ");
            }
        }
    }

    private Object lookupInternal(Name name) throws NamingException {
        Name parsedName = getParsedName(name);
        String nameComponent = parsedName.get(0);
        Object res = objects.get(nameComponent);

        // if not found
        if (!objects.containsKey(nameComponent)) {
            throw new NameNotFoundException("Name " + name + " not found.");
        }
        // if this is a compound name
        else if (parsedName.size() > 1) {

            if (res instanceof NamingContext) {
                res = ((NamingContext) res).lookupInternal(parsedName
                        .getSuffix(1));
            } else {
                throw new NotContextException("Expected NamingContext but found "
                        + res);
            }
        }
        // if this is a reference
        else if (res instanceof Reference) {
            try {
                Hashtable<String, Object> env = new Hashtable<String, Object>();
                res = NamingManager.getObjectInstance(res, name, this,
                        env);
                if (res != null) {
                    objects.put(nameComponent, res);
                }
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                throw new NamingException(e.getMessage());
            }
        }
        return res;
    }

    /**
     * Looks up the object in this context. If the object is not found and the
     * remote context was provided, calls the remote context to lookup the
     * object.
     *
     * @param name object to search
     * @return object reference bound to name <code>name</code>
     * @throws NamingException if naming error occurs
     * @see #lookup(javax.naming.Name)
     */
    public Object lookup(String name) throws NamingException {
        checkIsDestroyed();
        try {
            return lookupInternal(name);
        } catch (NameNotFoundException ex) {
            // Shall we delegate?
            if (remoteContext != null) {
                return remoteContext.lookup(name);
            } else {
                throw new NameNotFoundException("Name " + name + " not found. ");
            }
        }
    }

    private Object lookupInternal(String name) throws NamingException {
        return lookupInternal(nameParser.parse(name));
    }

    /**
     * Not implemented.
     *
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink(Name arg0) throws NamingException {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Not implemented.
     *
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    public Object lookupLink(String arg0) throws NamingException {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Rebinds object <code>obj</code> to name <code>name</code>. If there
     * is existing binding it will be overwritten.
     *
     * @param name name of the object to rebind
     * @param obj object to add. Can be <code>null</code>
     * @throws NoPermissionException if this context has been destroyed
     * @throws InvalidNameException if <code>name</code> is empty or is
     *             <code>CompositeName</code> that spans more than one naming
     *             system
     * @throws NotContextException if <code>name</code> has more than one
     *             atomic name and intermediate context is not found
     * @throws NamingException if any other naming error occurs
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind(Name name, Object obj) throws NamingException {
        checkIsDestroyed();
        Name parsedName = getParsedName(name);
        if (parsedName.isEmpty() || parsedName.get(0).length() == 0) {
            throw new InvalidNameException("Name can not be empty!");
        }
        String nameToBind = parsedName.get(0);

        if (parsedName.size() == 1) {
            objects.put(nameToBind, obj);
        } else {
            Object boundObject = objects.get(nameToBind);
            if (boundObject instanceof Context) {
                /*
                 * Let the subcontext bind the object.
                 */
                ((Context) boundObject).bind(parsedName.getSuffix(1), obj);
            } else {
                if (boundObject == null) {
                    // Create new subcontext and let it do the binding
                    Context sub = createSubcontext(nameToBind);
                    sub.bind(parsedName.getSuffix(1), obj);
                } else {
                    throw new NotContextException("Expected Context but found "
                            + boundObject);
                }
            }
        }
    }

    /**
     * Same as bind except that if <code>name</code> is already bound in the
     * context, it will be re-bound to object <code>obj</code>.
     *
     * @param name name of the object to rebind
     * @param obj object to add. Can be <code>null</code>
     * @throws NamingException if naming error occurs
     * @see #rebind(javax.naming.Name, Object)
     */
    public void rebind(String name, Object obj) throws NamingException {
        rebind(nameParser.parse(name), obj);
    }

    /**
     * Not implemented.
     *
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment(String arg0) throws NamingException {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Not implemented.
     *
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename(Name arg0, Name arg1) throws NamingException {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Not implemented.
     *
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename(String arg0, String arg1) throws NamingException {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Removes <code>name</code> and its associated object from the context.
     *
     * @param name name to remove
     * @throws NoPermissionException if this context has been destroyed
     * @throws InvalidNameException if <code>name</code> is empty or is
     *             <code>CompositeName</code> that spans more than one naming
     *             system
     * @throws NameNotFoundException if intermediate context can not be found
     * @throws NotContextException if <code>name</code> has more than one
     *             atomic name and intermediate context is not found
     * @throws NamingException if any other naming exception occurs
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind(Name name) throws NamingException {
        checkIsDestroyed();
        Name parsedName = getParsedName(name);
        if (parsedName.isEmpty() || parsedName.get(0).length() == 0) {
            throw new InvalidNameException("Name can not be empty!");
        }
        String nameToRemove = parsedName.get(0);

        if (parsedName.size() == 1) {
            objects.remove(nameToRemove);
        } else {
            Object boundObject = objects.get(nameToRemove);
            if (boundObject instanceof Context) {
                /*
                 * Let the subcontext do the unbind
                 */
                ((Context) boundObject).unbind(parsedName.getSuffix(1));
            } else {
                if (!objects.containsKey(nameToRemove)) {
                    throw new NameNotFoundException("Can not find " + name);
                }
                throw new NotContextException("Expected Context but found "
                        + boundObject);
            }
        }
    }

    /**
     * Removes object from the object map.
     *
     * @param name object to remove
     * @throws NamingException if naming error occurs
     * @see #unbind(javax.naming.Name)
     */
    public void unbind(String name) throws NamingException {
        unbind(nameParser.parse(name));
    }

    // ** Non-standard methods

    /**
     * Checks if this context has been destroyed. <code>isDestroyed</code> is
     * set to <code>true</code> when a context is destroyed by calling
     * <code>destroySubcontext</code> method.
     *
     * @throws NoPermissionException if this context has been destroyed
     */
    private void checkIsDestroyed() throws NoPermissionException {
        if (isDestroyed) {
            throw new NoPermissionException("Can not invoke operations on destroyed context!");
        }
    }

    /**
     * Marks this context as destroyed.
     * Method called only by <code>destroySubcontext</code>.
     */
    private void destroyInternal() {
        isDestroyed = true;
    }

    /**
     * Parses <code>name</code> which is <code>CompositeName</code> or
     * <code>CompoundName</code>. If <code>name</code> is not
     * <code>CompositeName</code> then it is assumed to be
     * <code>CompoundName</code>.
     * <p>
     * If the name contains leading and/or terminal empty components, they will
     * not be included in the result.
     *
     * @param name <code>Name</code> to parse
     * @return parsed name as instance of <code>CompoundName</code>
     * @throws InvalidNameException if <code>name</code> is
     *             <code>CompositeName</code> and spans more than one name
     *             space
     * @throws NamingException if any other naming exception occurs
     */
    private static Name getParsedName(Name name) throws NamingException {
        Name result;

        if (name instanceof CompositeName) {
            if (name.isEmpty()) {
                // Return empty CompositeName
                result = nameParser.parse("");
            } else if (name.size() > 1) {
                throw new InvalidNameException("Multiple name systems are not supported!");
            }
            result = nameParser.parse(name.get(0));
        } else {
            result = (Name) name.clone();
        }

        while (!result.isEmpty()) {
            if (result.get(0).length() == 0) {
                result.remove(0);
                continue;
            }
            if (result.get(result.size() - 1).length() == 0) {
                result.remove(result.size() - 1);
                continue;
            }
            break;
        }
        // Validate that there are not intermediate empty components.
        // Skip first and last element, they are valid
        for (int i = 1; i < result.size() - 1; i++) {
            if (result.get(i).length() == 0) {
                throw new InvalidNameException("Empty intermediate components are not supported!");
            }
        }
        return result;
    }

    /**
     * Returns the compound string name of this context.
     * Suppose a/b/c/d is the full name and this context is "c".
     * It's compound string name is a/b/c
     *
     * @return compound string name of the context
     */
    String getCompoundStringName() throws NamingException {
        //StringBuffer compositeName  = new StringBuffer();
        String compositeName="";
        NamingContext curCtx = this;
        while (!curCtx.isRootContext()) {
            compositeName = composeName(compositeName, curCtx.contextName);
            curCtx = curCtx.parent;
        }
        return compositeName;
    }

    /**
     * Returns parent context of this context.
     */
    NamingContext getParentContext() {
        return parent;
    }

    /**
     * Returns the "atomic" (as opposed to "composite") name of the context.
     *
     * @return name of the context
     */
    String getAtomicName(){
        return contextName;
    }

    /**
     * Returns true if this context is the root context.
     *
     * @return true if the context is the root context
     */
    boolean isRootContext(){
        return parent == null;
    }

    private static class NamingEnumerationImpl implements NamingEnumeration<Binding> {

        private final Vector<Binding> elements;
        private int currentElement;

        NamingEnumerationImpl(Vector<Binding> elements) {
            this.elements = elements;
            currentElement = 0;
        }

        public void close() {
            currentElement = 0;
            elements.clear();
        }

        public boolean hasMore() {
            return hasMoreElements();
        }

        public boolean hasMoreElements() {
            if (currentElement < elements.size()) {
                return true;
            }
            close();
            return false;
        }

        public Binding next() {
            return nextElement();
        }

        public Binding nextElement() {
            if (hasMoreElements()) {
                return elements.get(currentElement++);
            }
            throw new NoSuchElementException();
        }
    }

    @Override
    public String toString() {
        try {
            return getCompoundStringName();
        } catch (NamingException e) {
            return super.toString();
        }
    }

}
