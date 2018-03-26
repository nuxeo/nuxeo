/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.core.impl;

import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.CompiledChain;
import org.nuxeo.ecm.automation.ExitException;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.scripting.Expression;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class OperationChainCompiler {

    private static final Log log = LogFactory.getLog(OperationChainCompiler.class);

    protected final AutomationService service;

    protected final LoadingCache<Connector, OperationMethod> cache;

    protected static final int MAX_CACHE_SIZE = 1000;

    protected OperationChainCompiler(AutomationService service) {
        this.service = service;
        cache = CacheBuilder.newBuilder() //
                            .maximumSize(MAX_CACHE_SIZE)
                            .build(new CacheLoader<Connector, OperationMethod>() {
                                @Override
                                public OperationMethod load(Connector connector) throws OperationException {
                                    return connector.connect();
                                }
                            });
    }

    public CompiledChain compile(ChainTypeImpl typeof, Class<?> typein) throws OperationException {
        Connector connector = new Connector(typeof, typein);
        OperationMethod operationMethod;
        try {
            operationMethod = cache.get(connector);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OperationException) {
                throw (OperationException) cause;
            } else {
                throw new OperationException(cause);
            }
        }
        return new CompiledChainImpl(typeof, typein, operationMethod);
    }

    protected class Connector {

        protected final ChainTypeImpl typeof;

        protected final Class<?> typein;

        protected Connector(ChainTypeImpl typeof, Class<?> typein) {
            this.typeof = typeof;
            this.typein = typein;
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = prime * result + typeof.hashCode();
            result = prime * result + typein.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Connector)) {
                return false;
            }
            Connector other = (Connector) obj;
            return typeof.equals(other.typeof) && typein.equals(other.typein);
        }

        protected OperationMethod connect() throws OperationException {
            OperationMethod head = null;
            OperationMethod prev = null;
            for (OperationParameters params : typeof.chain.getOperations()) {
                OperationMethod next = new OperationMethod(params, prev);
                if (prev != null) {
                    prev.next = next;
                }
                if (next.prev == null) {
                    head = next;
                }
                prev = next;
            }
            if (head != null) {
                head.solve(typein);
            }
            return head;
        }
    }

    protected class OperationMethod {

        protected final OperationType typeof;

        protected final OperationParameters params;

        protected InvokableMethod method;

        protected OperationMethod prev;

        protected OperationMethod next;

        protected OperationMethod(OperationParameters params, OperationMethod prev) throws OperationNotFoundException {
            typeof = service.getOperation(params.id());
            this.params = params;
            this.prev = prev;
        }

        protected Object invoke(OperationContext context) throws OperationException {
            context.getCallback().onOperationEnter(context, typeof, method, params.map());
            Object output = method.invoke(context, params.map());
            if (output instanceof Expression) {
                output = ((Expression) output).eval(context);
            }
            context.getCallback().onOperationExit(output);
            context.setInput(output);
            if (next != null) {
                return next.invoke(context);
            }
            return output;
        }

        /**
         * Compute the best matching path to perform the chain of operations. The path is computed using a backtracking
         * algorithm.
         *
         * @throws InvalidChainException
         */
        void solve(Class<?> in) throws InvalidChainException {
            InvokableMethod[] methods = typeof.getMethodsMatchingInput(in);
            if (methods.length == 0) {
                throw new InvalidChainException(
                        "Cannot find any valid path in operation chain - no method found for operation '"
                                + typeof.getId() + "' and for first input type '" + in.getName() + "'");
            }
            if (next == null) {
                method = methods[0];
                return;
            }
            for (InvokableMethod m : methods) {
                Class<?> nextIn = m.getOutputType();
                if (nextIn == Void.TYPE || nextIn.equals(Object.class)) {
                    nextIn = in; // preserve last input
                }
                try {
                    next.solve(nextIn);
                    method = m;
                    return;
                } catch (InvalidChainException cause) {
                    // continue solving
                }
            }
            throw new InvalidChainException(
                    "Cannot find any valid path in operation chain - no method found for operation '" + typeof.getId()
                            + "' and for first input type '" + in.getName() + "'");
        }
    }

    protected class CompiledChainImpl implements CompiledChain {

        protected final ChainTypeImpl typeof;

        protected final Class<?> typein;

        protected final OperationMethod head;

        protected CompiledChainImpl(ChainTypeImpl typeof, Class<?> typein, OperationMethod head) {
            this.typeof = typeof;
            this.typein = typein;
            this.head = head;
        }

        @Override
        public Object invoke(OperationContext ctx) throws OperationException {
            return ctx.callWithChainParameters(() -> {
                ctx.getCallback().onChainEnter(typeof);
                try {
                    return head.invoke(ctx);
                } catch (ExitException e) {
                    if (e.isRollback()) {
                        ctx.setRollback();
                    }
                    return ctx.getInput();
                } catch (OperationException op) {
                    throw ctx.getCallback().onError(op);
                } finally {
                    ctx.getCallback().onChainExit();
                }
            }, typeof.getChainParameters());
        }

        @Override
        public String toString() {
            return "CompiledChainImpl [op=" + typeof + "," + "input=" + typein + "]";
        }

    }

}