/*
 * (C) Copyright 2013-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Guillaume Renard
 *
 */
package org.nuxeo.ecm.automation;

import java.util.List;
import java.util.Map;

/**
 * Service providing operation execution methods.
 * <p>
 * Progress monitor for asynchronous executions is not implemented.
 */
public interface AutomationService {

    /**
     * Gets all operation types that was registered.
     */
    OperationType[] getOperations();

    /**
     * Gets an operation type given its ID. Throws an exception if the operation is not found.
     */
    OperationType getOperation(String id) throws OperationNotFoundException;

    /**
     * Builds the operation chain given a context. If the context input object or the chain cannot be resolved (no path
     * can be found through all the operation in the chain) then {@link InvalidChainException} is thrown. The returned
     * object can be used to run the chain.
     */
    CompiledChain compileChain(Class<?> inputType, OperationChain chain) throws OperationException;

    /**
     * Same as previous but takes an array of operation parameters
     */
    CompiledChain compileChain(Class<?> inputType, OperationParameters... chain) throws OperationException;

    /**
     * Builds and runs the operation chain given a context. If the context input object or the chain cannot be resolved
     * (no path can be found through all the operation in the chain) then {@link InvalidChainException} is thrown.
     */
    Object run(OperationContext ctx, OperationChain chain) throws OperationException;

    /**
     * Same as previous but for managed chains identified by an ID. For managed chains always use this method since the
     * compiled chain is cached and run will be faster
     */
    Object run(OperationContext ctx, String chainId) throws OperationException;

    /**
     * Shortcut to execute a single operation described by the given ID and map of parameters
     */
    Object run(OperationContext ctx, String id, Map<String, ?> params) throws OperationException;

    /**
     * Gets a registered operation chain.
     *
     * @deprecated since 5.9.2 no specific chain registry anymore: chains are now operations, use
     *             {@link #getOperation(String)} method instead.
     * @since 5.7.2
     */
    @Deprecated(since = "5.9.2")
    OperationChain getOperationChain(String id) throws OperationNotFoundException;

    /**
     * Gets a list of all registered chains
     *
     * @deprecated since 5.9.2 no specific chain registry anymore: chains are now operations, use
     *             {@link #getOperations()} method instead.
     * @since 5.7.2
     * @return the list or an empty list if no registered chains exists
     */
    @Deprecated(since = "5.9.2")
    List<OperationChain> getOperationChains();

    /**
     * Gets a type adapter for the input type accept and the output type produce. Returns null if no adapter was
     * registered for these types.
     */
    TypeAdapter getTypeAdapter(Class<?> accept, Class<?> produce);

    /**
     * Adapts an object to a target type if possible otherwise throws an exception. The method must be called in an
     * operation execution with a valid operation context.
     */
    <T> T getAdaptedValue(OperationContext ctx, Object toAdapt, Class<?> targetType) throws OperationException;

    /**
     * Checks whether or not the given type is adaptable into the target type. An instance of an adaptable type can be
     * converted into an instance of the target type.
     * <p>
     * This is a shortcut to <code>getTypeAdapter(typeToAdapt, targetType) != null</code>
     */
    boolean isTypeAdaptable(Class<?> typeToAdapt, Class<?> targetType);

    /**
     * Generates a documentation model for all registered operations. The documentation model is generated from
     * operation annotations and can be used in UI tools to describe operations. The returned list is sorted using
     * operation ID. Optional method.
     */
    List<OperationDocumentation> getDocumentation() throws OperationException;

    /**
     * @since 5.7.2
     * @param id operation ID
     * @return true if operation registry contains the given operation
     */
    boolean hasOperation(String id);

    /**
     * @since 5.7.3
     */
    ChainException[] getChainExceptions();

    /**
     * @since 5.7.3
     */
    ChainException getChainException(String onChainId);

    /**
     * @since 5.7.3
     */
    AutomationFilter getAutomationFilter(String id);

    /**
     * @since 5.7.3
     */
    AutomationFilter[] getAutomationFilters();

    /**
     * @since 5.7.3
     */
    boolean hasChainException(String onChainId);

    /**
     * This running method execute operation process through a new transaction.
     *
     * @param ctx the operation context.
     * @param chainId the chain Id.
     * @param chainParameters chain parameters.
     * @param timeout Transaction timeout.
     * @param rollbackGlobalOnError Rollback or not transaction after failing.
     * @since 6.0
     */
    Object runInNewTx(OperationContext ctx, String chainId, Map<String, ?> chainParameters, Integer timeout,
            boolean rollbackGlobalOnError) throws OperationException;

}
