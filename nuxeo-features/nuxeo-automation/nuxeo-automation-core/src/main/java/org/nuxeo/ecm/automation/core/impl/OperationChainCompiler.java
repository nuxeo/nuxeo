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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.CompiledChain;
import org.nuxeo.ecm.automation.ExitException;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;

class OperationChainCompiler {

    final AutomationService service;

    final Map<Key, CompiledChainImpl> cache = new ConcurrentHashMap<>();

    OperationChainCompiler(AutomationService service) {
        this.service = service;
    }

    CompiledChain compile(ChainTypeImpl typeof, Class<?> typein) throws OperationException {
        Key key = new Key(typeof, typein);
        if (!cache.containsKey(key)) {
            cache.put(key, compile(key));
        }
        return cache.get(key);
    }

    CompiledChainImpl compile(Key key) throws OperationException {
        OperationMethod head = null;
        OperationMethod prev = null;
        for (OperationParameters params : key.typeof.chain.getOperations()) {
            OperationMethod next = new OperationMethod(service.getOperation(params.id()), params.map(), prev);
            if (prev != null) {
                prev.next = next;
            }
            if (next.prev == null) {
                head = next;
            }
            prev = next;
        }
        initializePath(head, key.typein);
        return new CompiledChainImpl(key, head);
    }

    /**
     * Compute the best matching path to perform the chain of operations. The path is computed using a backtracking
     * algorithm.
     *
     * @throws InvalidChainException
     */
    void initializePath(OperationMethod element, Class<?> in) throws InvalidChainException {
        InvokableMethod[] methods = element.typeof.getMethodsMatchingInput(in);
        if (methods == null) {
            throw new InvalidChainException(
                    "Cannot find any valid path in operation chain - no method found for operation '"
                            + element.typeof.getId() + "' and for first input type '" + in.getName() + "'");
        }
        if (element.next == null) {
            element.method = methods[0];
            return;
        }
        for (InvokableMethod m : methods) {
            Class<?> nextIn = m.getOutputType();
            if (nextIn == Void.TYPE || nextIn.equals(Object.class)) {
                nextIn = in; // preserve last input
            }
            try {
                initializePath(element.next, nextIn);
                element.method = m;
                return;
            } catch (InvalidChainException cause) {
                ;
            }
        }
        throw new InvalidChainException(
                "Cannot find any valid path in operation chain - no method found for operation '"
                        + element.typeof.getId() + "' and for first input type '" + in.getName() + "'");
    }

    class Key {
        final ChainTypeImpl typeof;
        final Class<?> typein;
        final int hashcode;

        Key(ChainTypeImpl typeof, Class<?> typein) {
            this.typeof = typeof;
            this.typein = typein;
            hashcode = hashcode(typeof, typein);
        }

        int hashcode(OperationType typeof, Class<?> typein) {
            int prime = 31;
            int result = 1;
            result = prime * result + typeof.hashCode();
            result = prime * result + typein.hashCode();
            return result;
        }

        @Override
        public int hashCode() {
            return hashcode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            Key other = (Key) obj;
            if (!typein.equals(other.typein)) {
                return false;
            }
            if (!typeof.equals(other.typeof)) {
                return false;
            }
            return true;
        }
    }

    static class OperationMethod {

        final OperationType typeof;

        final Map<String, Object> args = new HashMap<>();

        InvokableMethod method;

        OperationMethod prev;
        OperationMethod next;

        OperationMethod(OperationType typeof, Map<String, ?> args, OperationMethod prev) {
            this.typeof = typeof;
            this.args.putAll(args);
            this.prev = prev;
        }

        Object invoke(OperationContext context) throws OperationException {
            context.getCallback().onOperationEnter(context, typeof, method, args);
            Object output = method.invoke(context, args);
            context.getCallback().onOperationExit(output);
            context.setInput(output);
            if (next != null) {
                return next.invoke(context);
            }
            return output;
        }
    }


    class CompiledChainImpl implements CompiledChain {

        final Key key;

        final OperationMethod head;

        CompiledChainImpl(Key key, OperationMethod head) {
            this.key = key;
            this.head = head;
        }

        @Override
        public Object invoke(OperationContext ctx) throws OperationException {
            ctx.getCallback().onChainEnter(key.typeof);
            try {
                return head.invoke(ctx);
            } catch (ExitException e) {
                if (e.isRollback()) {
                    ctx.setRollback();
                }
                return ctx.getInput();
            } finally {
                ctx.getCallback().onChainExit();
            }
        }

        @Override
        public String toString() {
            return "CompiledChainImpl [op=" + key.typeof + "," + "input=" + key.typein + "]";
        }

    }

}