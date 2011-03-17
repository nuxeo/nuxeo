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
package org.nuxeo.ecm.automation;

import java.util.List;

import org.nuxeo.ecm.automation.core.annotations.Operation;

/**
 * Service providing an operation registry and operation execution methods. The
 * operation registry is thread-safe and optimized for lookups. Progress monitor
 * for asynchronous executions is not yet implemented.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface AutomationService {

    /**
     * Registers an operation given its class. The operation class MUST be
     * annotated using {@link Operation} annotation. If an operation having the
     * same ID exists an exception will be thrown.
     */
    void putOperation(Class<?> type) throws OperationException;

    /**
     * Registers an operation given its class. The operation class MUST be
     * annotated using {@link Operation} annotation. If the <code>replace</code>
     * argument is true then any existing operation having the same ID will
     * replaced with this one.
     */
    void putOperation(Class<?> type, boolean replace) throws OperationException;

    /**
     * Removes an operation given its class. If the operation was not registered
     * does nothing.
     */
    void removeOperation(Class<?> key);

    /**
     * Gets all operation types that was registered.
     */
    OperationType[] getOperations();

    /**
     * Gets an operation type given its ID. Throws an exception if the operation
     * is not found.
     */
    OperationType getOperation(String id) throws OperationNotFoundException;

    /**
     * Builds the operation chain given a context. If the context input object
     * or the chain cannot be resolved (no path can be found through all the
     * operation in the chain) then {@link InvalidChainException} is thrown. The
     * returned object can be used to run the chain.
     */
    CompiledChain compileChain(Class<?> inputType, OperationChain chain)
            throws Exception, InvalidChainException;

    /**
     * Same as previous but takes an array of operation parameters
     */
    CompiledChain compileChain(Class<?> inputType, OperationParameters... chain)
            throws Exception, InvalidChainException;

    /**
     * Builds and runs the operation chain given a context. If the context input
     * object or the chain cannot be resolved (no path can be found through all
     * the operation in the chain) then {@link InvalidChainException} is thrown.
     */
    Object run(OperationContext ctx, OperationChain chain)
            throws OperationException, InvalidChainException, Exception;

    /**
     * Same as previous but for managed chains identified by an ID. For managed
     * chains always use this method since the compiled chain is cached and run
     * will be faster
     */
    Object run(OperationContext ctx, String chainId) throws OperationException,
            InvalidChainException, Exception;

    /**
     * Registers a parametrized operation chain. This chain can be executed
     * later by calling <code>run</code> and passing the chain ID. If a chain
     * having the same ID exists an exception is thrown
     */
    void putOperationChain(OperationChain chain) throws OperationException;

    /**
     * Registers a parametrized operation chain. This chain can be executed
     * later by calling <code>run</code> and passing the chain ID. If the
     * replace attribute is true then any chain already registered under the
     * same id will be replaced otherwise an exception is thrown.
     */
    void putOperationChain(OperationChain chain, boolean replace)
            throws OperationException;

    /**
     * Removes a registered operation chain given its ID. Do nothing if the
     * chain was not registered.
     */
    void removeOperationChain(String id);

    /**
     * Gets a registered operation chain.
     */
    OperationChain getOperationChain(String id)
            throws OperationNotFoundException;

    /**
     * Gets a list of all registered chains
     *
     * @return the list or an empty list if no registered chains exists
     */
    List<OperationChain> getOperationChains();

    /**
     * Registers a new type adapter that can adapt an instance of the accepted
     * type into one of the produced type.
     */
    void putTypeAdapter(Class<?> accept, Class<?> produce, TypeAdapter adapter);

    /**
     * Removes a type adapter
     */
    void removeTypeAdapter(Class<?> accept, Class<?> produce);

    /**
     * Gets a type adapter for the input type accept and the output type
     * produce. Returns null if no adapter was registered for these types.
     */
    TypeAdapter getTypeAdapter(Class<?> accept, Class<?> produce);

    /**
     * Adapts an object to a target type if possible otherwise throws an
     * exception. The method must be called in an operation execution with a
     * valid operation context.
     */
    <T> T getAdaptedValue(OperationContext ctx, Object toAdapt,
            Class<?> targetType) throws Exception;

    /**
     * Checks whether or not the given type is adaptable into the target type.
     * An instance of an adaptable type can be converted into an instance of the
     * target type.
     * <p>
     * This is a shortcut to
     * <code>getTypeAdapter(typeToAdapt, targetType) != null</code>
     */
    boolean isTypeAdaptable(Class<?> typeToAdapt, Class<?> targetType);

    /**
     * Generates a documentation model for all registered operations. The
     * documentation model is generated from operation annotations and can be
     * used in UI tools to describe operations. The returned list is sorted
     * using operation ID. Optional method.
     */
    List<OperationDocumentation> getDocumentation();

}
