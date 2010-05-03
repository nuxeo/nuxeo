/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation;

import java.util.List;

import org.nuxeo.ecm.automation.core.annotations.Operation;

/**
 * Service providing an operation registry and operation execution methods.
 * The operation registry is thread safe and optimized for lookups.
 * 
 * Progress monitor for asynchronous executions is not yet implemented. 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface AutomationService {
    
    /**
     * Register an operation given its class. The operation class MUST be annotated using {@link Operation} annotation.
     * If an operation having the same ID exists an exception will be thrown.
     * @param type
     * @throws OperationException
     */
    public void putOperation(Class<?> type) throws OperationException;
    
    /**
     * Register an operation given its class. The operation class MUST be annotated using {@link Operation} annotation.
     * If the <code>replace</code> argument is true then any existing operation having the same ID 
     * will replaced with this one.
     * @param type
     * @param replace
     * @throws OperationException
     */
    public void putOperation(Class<?> type, boolean replace) throws OperationException;

    /**
     * Remove an operation given its class.
     * If the operation was not registered does nothing.
     * @param key
     */
    public void removeOperation(Class<?> key);
    
    /**
     * Get all operation types that was registered.  
     * @return
     */
    public OperationType[] getOperations();

    /**
     * Get an operation type given its ID. Throws an exception if the operation is not found.
     * @param id
     * @return
     * @throws OperationException
     */
    public OperationType getOperation(String id) throws OperationException;
    
    /**
     * Build the operation chain given a context. If the context input object or 
     * the chain cannot be resolved (no path can be found through all the operation in the chain) then
     * {@link InvalidChainException} is thrown.
     * The returned object can be used to run the chain.   
     * @param input
     * @param chain
     * @return
     * @throws Exception
     * @throws InvalidChainException
     */
    public CompiledChain compileChain(Class<?> inputType, OperationChain chain) throws Exception, InvalidChainException;
    
    /**
     * Same as previous but takes an array of operation parameters
     * @param inputType
     * @param chain
     * @return
     * @throws Exception
     * @throws InvalidChainException
     */
    public CompiledChain compileChain(Class<?> inputType, OperationParameters ... chain) throws Exception, InvalidChainException;
    
    /**
     * Build and run the operation chain given a context. If the context input object or 
     * the chain cannot be resolved (no path can be found through all the operation in the chain) then
     * {@link InvalidChainException} is thrown.
     * @param ctx
     * @return
     * @throws Exception
     * @throws InvalidChainException
     */
    public Object run(OperationContext ctx, OperationChain chain) throws Exception, InvalidChainException;
    
    /**
     * Same as previous but for managed chains identified by an ID.
     * For managed chains always use this method since the compiled chain is cached and run will be faster
     * @param ctx
     * @param chainId
     * @return
     * @throws Exception
     * @throws InvalidChainException
     */
    public Object run(OperationContext ctx, String chainId) throws Exception, InvalidChainException;
    
    
    /**
     * Register a parameterized operation chain. This chain can be executed later by calling <code>run</code> and passing the chain ID.
     * If a chain having the same ID exists an exception is thrown
     * @param chain
     */
    public void putOperationChain(OperationChain chain) throws OperationException;
    
    /**
     * Register a parameterized operation chain. This chain can be executed later by calling <code>run</code> and passing the chain ID.
     * If the replace attribute is true then any chain already registered under the same id will be replaced otherwise
     * an exception is thrown.
     * @param chain
     * @param replace
     * @throws OperationException
     */
    public void putOperationChain(OperationChain chain, boolean replace) throws OperationException;

    /**
     * Remove a registered operation chain given its ID. Do nothing if the chain was not registered. 
     */
    public void removeOperationChain(String id);
    
    /**
     * Get a registered operation chain.
     * @param id
     */
    public OperationChain getOperationChain(String id) throws OperationException;
    
    /**
     * Register a new type adapter that can adapt an instance of the accepted type into one of the produced type.
     * @param accept
     * @param produce
     * @param adapter
     */
    public void putTypeAdapter(Class<?> accept, Class<?> produce, TypeAdapter adapter);
    
    /**
     * Remove a type adapter
     * @param accept
     * @param produce
     */
    public void removeTypeAdapter(Class<?> accept, Class<?> produce);
    
    /**
     * Get a type adapter for the input type accept and the output type produce. 
     * Return null if no adapter was registered for these types. 
     * @param accept
     * @param produce
     * @return
     */
    public TypeAdapter getTypeAdapter(Class<?> accept, Class<?> produce);
    
    /**
     * Adapt an object to a target type if possible otherwise throws an exception.
     * The method must be called in an operation execution with a valid operation context.
     * @param <T>
     * @param ctx
     * @param toAdapt
     * @param targetType
     * @return
     * @throws Exception
     */
    public <T> T getAdapter(OperationContext ctx, Object toAdapt, Class<?> targetType) throws Exception;
    
    /**
     * Check whether or not the given type is adaptable into the target type.
     * An instance of an adaptable type can be converted into an instance of the target type.
     * <p>
     * This is a shortcut to <code>getTypeAdapter(typeToAdapt, targetType) != null</code> 
     * @param typeToAdapt
     * @param targetType
     * @return
     * @throws Exception
     */
    public boolean isTypeAdaptable(Class<?> typeToAdapt, Class<?> targetType);
    
    
    /**
     * Generate a documentation model for all registered operations.
     * The documentation model is generated from operation annotations and can be used in UI tools to describe operations.
     * 
     * The returned list is sorted using operation ID.
     * 
     * Optional method.
     *  
     * @return
     */
    public List<OperationDocumentation> getDocumentation();
    
}
