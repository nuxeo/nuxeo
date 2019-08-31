/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.api.login;

import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.Collections;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import sun.reflect.ReflectionFactory;

/**
 * An implementation of {@link LoginContext} just holding a principal.
 * <p>
 * Construction is done through the static {@link #create} method, which takes a {@link Principal}. The caller must push
 * the principal on the Nuxeo principal stack using {@link #login}, and finally do {@link #logout} or {@link #close} to
 * remove it.
 * <p>
 * This is used for compatibility with previous code that expected to receive a {@link LoginContext} and then call
 * {@link #logout} on it.
 *
 * @since 11.1
 */
public class NuxeoLoginContext extends LoginContext implements AutoCloseable {

    protected Principal principal;

    protected Subject subject;

    protected boolean loggedIn;

    /**
     * This constructor cannot be used, use the static method {@link #create} instead.
     */
    public NuxeoLoginContext() throws LoginException {
        super(error());
    }

    private static String error() {
        throw new UnsupportedOperationException("must use create() method");
    }

    // constructs the class without calling its declared constructor or its super's constructor
    // we need to do this because LoginContext construction is very costly
    private static final Constructor<?> BARE_CONSTRUCTOR;
    static {
        try {
            Class<?> klass = NuxeoLoginContext.class;
            BARE_CONSTRUCTOR = ReflectionFactory.getReflectionFactory()
                                                .newConstructorForSerialization(klass,
                                                        Object.class.getDeclaredConstructor());
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Creates a {@link NuxeoLoginContext} for the given principal.
     *
     * @param principal the principal
     * @return the login context
     */
    public static NuxeoLoginContext create(Principal principal) {
        NuxeoLoginContext loginContext;
        try {
            loginContext = (NuxeoLoginContext) BARE_CONSTRUCTOR.newInstance();
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        loginContext.setPrincipal(principal);
        return loginContext;
    }

    protected void setPrincipal(Principal principal) {
        this.principal = principal;
        this.subject = new Subject(true, Collections.singleton(principal), Collections.emptySet(),
                Collections.emptySet());
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public void login() {
        if (!loggedIn) {
            LoginComponent.pushPrincipal(principal);
            loggedIn = true;
        }
    }

    @Override
    public void logout() {
        close();
    }

    @Override
    public void close() {
        if (loggedIn) {
            LoginComponent.popPrincipal();
            loggedIn = false;
        }
    }

}
