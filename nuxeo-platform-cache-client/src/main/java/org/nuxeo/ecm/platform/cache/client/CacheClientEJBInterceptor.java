/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: LogEntryCallbackListener.java 16046 2007-04-12 14:34:58Z fguillaume $
 */
package org.nuxeo.ecm.platform.cache.client;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.nuxeo.ecm.platform.cache.CacheableObjectKeys;
import org.nuxeo.ecm.platform.cache.CacheServiceException;
import org.nuxeo.ecm.platform.cache.data.DocumentModelGhost;

/**
 * A client-side EJB interceptor that checks the caller and if the case
 * retrieves the required result from local client cache thus by-passing the
 * call to the server side of the EJB.
 *
 * @author DM
 */
public class CacheClientEJBInterceptor implements Interceptor, Serializable {

    // FIXME: hardcoded method signatures
    static final String PARAM_TYPE_DOC_REF = "org.nuxeo.ecm.core.api.DocumentRef";
    static final String PARAM_TYPE_DOC_VER = "org.nuxeo.ecm.core.api.VersionModel";

    private static final long serialVersionUID = 5812846857293207784L;

    private static final Log log = LogFactory.getLog(CacheClientEJBInterceptor.class);

    /**
     * @see org.jboss.aop.advice.Interceptor#getName()
     */
    public String getName() {
        return CacheClientEJBInterceptor.class.getSimpleName();
    }

    /**
     * Actually dispatches the invocation to a more appropriate handler like
     * <code>handleMethodInvocation</code> for invocations of the specific
     * type.
     *
     * @see org.jboss.aop.advice.Interceptor#invoke(org.jboss.aop.joinpoint.Invocation)
     */
    public Object invoke(Invocation invocation) throws Throwable {
        log.debug("invocation enter: " + invocation);

        if (invocation instanceof MethodInvocation) {
            return handleMethodInvokation((MethodInvocation) invocation);
        }

        final Object obj = invocation.invokeNext();

        log.debug("invocation return: " + obj);

        return obj;
    }

    /**
     *
     * @param invocation
     * @return
     * @throws Throwable
     */
    private synchronized Object handleMethodInvokation(
            final MethodInvocation invocation) throws Throwable {

        final Object result;

        final Method method = invocation.getMethod();
        final Annotation[] anns = method.getAnnotations(); // not null
        final Object[] args = invocation.getArguments(); // could be null

        log.debug("invoked method             : " + method);
        log.debug("invoked method annotations : " + Arrays.asList(anns));
        if (args == null) {
            // not interesting from the cache point of view - cannot obtain
            // anything
            log
                    .debug("no arguments method - no cache object required. pass over the invocation.");
            result = invocation.invokeNext();
        } else {
            log.debug("invoked method  arguments  : " + Arrays.asList(args));

            // check annotation
            // final String fqn = getKeyFromInvocation(invocation, method);
            final String fqn = getKeyFromInvocationSafe(invocation, method);
            if (fqn == null) {
                // we pass on the request
                result = invocation.invokeNext();
            } else {

                final Object cachedObject = getObjectFromCache(fqn, method);

                if (cachedObject == null) {
                    log.debug("Object not found in cache. Invoking next...");

                    result = invocation.invokeNext();
                } else {
                    // we have a valid object from the cache
                    log.info("Found object in client cache: " + cachedObject);

                    result = cachedObject;
                }
            }
        }

        log.debug("Retrieved object: " + result);

        return result;
    }

    //
    // TODO : involving annotation verification along with cache usage - will
    // blow up the thread
    //
    /**
     *
     * @param invocation
     * @param method
     * @return the key to search by on the cache , <code>null</code> if it is
     *         not the case to search the object in the cache...
     */
    private String getKeyFromInvocation(MethodInvocation invocation,
            Method method) {

        final String key;
        if (method.isAnnotationPresent(ClientCacheIntercept.class)) {
            // the method is marked so its return value should be
            // checked for in the cache first
            log.debug("ClientCacheIntercept annotation detected");

            final ClientCacheIntercept ann = method
                    .getAnnotation(ClientCacheIntercept.class);

            // FIXME: get a real fqn...
            key = invocation.getArguments()[0].toString();
        } else {
            log.debug("ClientCacheIntercept annotation not detected. "
                    + "Ignoring call...");

            key = null;
        }

        return key;
    }

    //
    // public DocumentModel getDocument(DocumentRef docRef)
    // public DocumentModel getDocument(DocumentRef docRef, String[] schemas)
    // public DocumentModel getChild(DocumentRef parent, String name)
    // public DocumentModel getDocumentWithVersion(DocumentRef docRef,
    // VersionModel version)

    private static String getKeyFromInvocationSafe(MethodInvocation invocation,
            Method method) {

        final String methodName = method.getName();

        log.debug("Verify method: " + methodName);

        final String key;
        // check for signature : getDocument(DocumentRef docRef)
        // or : getDocument(DocumentRef docRef, String[] schemas)
        if (methodName.equals("getDocument")) {
            log.debug("Method candidate for check.");

            Class[] paramTypes = method.getParameterTypes();
            if (paramTypes[0].getName().equals(PARAM_TYPE_DOC_REF)) {

                final Object param0 = invocation.getArguments()[0];
                log.debug("Verify method, param ok: " + param0);

                key = CacheableObjectKeys.getCacheKey(param0);
            } else {
                log.debug("Verify method, param not ok: " + paramTypes[0]);

                key = null;
            }

        } else if (methodName.equals("getChild")) {

            Class[] paramTypes = method.getParameterTypes();
            if (paramTypes[0].getName().equals(PARAM_TYPE_DOC_REF)
                    && paramTypes[1].getName().equals(PARAM_TYPE_DOC_VER)) {

                final Object param0 = invocation.getArguments()[0];
                final Object param1 = invocation.getArguments()[1];
                log.debug("Verify method, param ok: " + param0 + ", "
                        + param1);

                key = CacheableObjectKeys.getCacheKey(param0, param1);
            } else {
                log.debug("Verify method, param not ok: " + paramTypes[0]);

                key = null;
            }
        } else if (methodName.equals("getDocumentWithVersion")) {

            Class[] paramTypes = method.getParameterTypes();
            if (paramTypes[0].getName().equals(PARAM_TYPE_DOC_REF)
                    && paramTypes[1].equals(String.class)) {

                final Object param0 = invocation.getArguments()[0];
                final Object param1 = invocation.getArguments()[1];
                log.debug("Verify method, param ok: " + param0 + ", "
                        + param1);

                // FIXME: key based on toString() is not ok
                // key = param0.toString();
                key = CacheableObjectKeys.getCacheKey(param0, (String) param1);
            } else {
                log.debug("Verify method, param not ok: " + paramTypes[0]);

                key = null;
            }
        } else if (methodName.equals("getChildren")) {
            log.debug("Method candidate for check.");

            Class[] paramTypes = method.getParameterTypes();
            if (paramTypes[0].getName().equals(PARAM_TYPE_DOC_REF)) {

                final Object param0 = invocation.getArguments()[0];
                log.debug("Verify method, param ok: " + param0);

                key = CacheableObjectKeys.getCacheKey(param0) + "/children";
            } else {
                log.debug("Verify method, param not ok: " + paramTypes[0]);

                key = null;
            }
        } else {
            // the method is not interesting
            log.debug("Method not involved in the cache.");

            key = null;
        }
        return key;
    }

    /**
     * Validates the type of the object if found in the cache.
     *
     * @param fqn the key the object is searched in cache by
     * @param method
     * @return
     * @throws CacheServiceException
     * @throws ClassCastException
     *             if the object found in cache is not of the expected type....
     */
    private static Object getObjectFromCache(String fqn, Method method)
            throws CacheServiceException {
        log.debug("Search the cache for object with FQN: " + fqn);

        final Object cachedObject = ClientCacheServiceFactory.getCacheService()
                .getObject(fqn);

        log.debug("Object from cache: " + cachedObject);

        if (cachedObject != null) {

            // check the object is a DocumentModelGhost !!!
            if (cachedObject instanceof DocumentModelGhost) {
                log.debug("Object from cache is a valid " + "DocumentModelGhost.");

                final DocumentModelGhost ghost = (DocumentModelGhost) cachedObject;

                //ghost.setDocumentManager(null);

                return ghost;
            }

            // fallback case:

            // check the object to be of the expected type
            final Class requiredRetType = method.getReturnType();
            if (!requiredRetType.isInstance(cachedObject)) {
                throw new ClassCastException(
                        "Cached object type inconsistency. Required: "
                                + requiredRetType + ", found: "
                                + cachedObject.getClass());
            }

            log.debug("Object from cache is valid [" + requiredRetType + ']');
            return cachedObject;
        }

        return null;
    }

}
