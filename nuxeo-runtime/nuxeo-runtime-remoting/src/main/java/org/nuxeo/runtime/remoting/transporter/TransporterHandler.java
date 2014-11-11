/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.nuxeo.runtime.remoting.transporter;

import java.lang.reflect.Method;

import javax.management.MBeanServer;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;

/**
 * Simple handler that uses reflection to make calls on target POJO (as supplied
 * in the constructor) when receive invocation requests.
 * <p>
 * Updated from jboss-remoting-2.0.0 - to handle primitive types
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class TransporterHandler implements ServerInvocationHandler {

    private final Object targetPOJO;

    public TransporterHandler(Object target) {
        targetPOJO = target;
    }

    /**
     * Takes the invocation request, which should have a internal parameter of
     * NameBasedInvocation, and converts that to a method call on the target POJO
     * (using reflection). Then returns the Object returned from the method call
     * on the target POJO.
     */
    @Override
    public Object invoke(InvocationRequest invocation) throws Throwable {
        Object request = invocation.getParameter();

        // Am expecting a NameBasedInvocation as the parameter
        NameBasedInvocation nbInvocation = (NameBasedInvocation) request;

        String methodName = nbInvocation.getMethodName();
        Object[] params = nbInvocation.getParameters();
        String[] sig = nbInvocation.getSignature();
        Class[] classSig = new Class[sig.length];
        for (int x = 0; x < sig.length; x++) {
            Class signature = getPrimitiveType(sig[x]);
            if (signature != null) {
                classSig[x] = signature;
            } else {
                classSig[x] = Class.forName(sig[x]);
            }
        }

        // use reflection to make the call
        Method method = targetPOJO.getClass().getMethod(methodName, classSig);
        return method.invoke(targetPOJO, params);
    }

    /**
     * Sets the mbean server that the handler can reference.
     */
    @Override
    public void setMBeanServer(MBeanServer server) {
        // NOOP
    }

    /**
     * Sets the invoker that owns this handler.
     */
    @Override
    public void setInvoker(ServerInvoker invoker) {
        // NOOP
    }

    /**
     * Adds a callback handler that will listen for callbacks from the server
     * invoker handler.
     */
    @Override
    public void addListener(InvokerCallbackHandler callbackHandler) {
        // NOOP
    }

    /**
     * Removes the callback handler that was listening for callbacks from the
     * server invoker handler.
     */
    @Override
    public void removeListener(InvokerCallbackHandler callbackHandler) {
        //NOOP
    }

    private static Class getPrimitiveType(String name) {
        if (name.equals("byte")) {
            return Byte.TYPE;
        }
        if (name.equals("short")) {
            return Short.TYPE;
        }
        if (name.equals("int")) {
            return Integer.TYPE;
        }
        if (name.equals("long")) {
            return Long.TYPE;
        }
        if (name.equals("char")) {
            return Character.TYPE;
        }
        if (name.equals("float")) {
            return Float.TYPE;
        }
        if (name.equals("double")) {
            return Double.TYPE;
        }
        if (name.equals("boolean")) {
            return Boolean.TYPE;
        }
        if (name.equals("void")) {
            return Void.TYPE;
        }
        return null;
    }

}
