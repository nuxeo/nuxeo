/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.common.utils;

import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides utility methods for manipulating and examining exceptions in a generic way.
 *
 * @author DM
 */
public final class ExceptionUtils {

    // This is an utility class.
    private ExceptionUtils() {
    }

    /**
     * Gets the root cause of the given <code>Throwable</code>.
     * <p>
     * This method walks through the exception chain up to the root of the exceptions tree using
     * {@link Throwable#getCause()}, and returns the root exception.
     *
     * @param throwable the throwable to get the root cause for, may be null - this is to avoid throwing other
     *            un-interesting exception when handling a business-important exception
     * @return the root cause of the <code>Throwable</code>, <code>null</code> if none found or null throwable input
     */
    public static Throwable getRootCause(Throwable throwable) {
        // This code is taken from Apache commons utils org.apache.commons.lang3.exception.ExceptionUtils
        final List<Throwable> list = getThrowableList(throwable);
        return list.size() < 2 ? null : list.get(list.size() - 1);
    }

    public static List<Throwable> getThrowableList(Throwable throwable) {
        final List<Throwable> list = new ArrayList<>();
        while (throwable != null && !list.contains(throwable)) {
            list.add(throwable);
            throwable = getCause(throwable);
        }
        return list;
    }

    protected static Throwable getCause(Throwable throwable) {
        if (throwable != null) {
            return throwable.getCause();
        }
        return null;
    }

    /**
     * Throws a {@link RuntimeException} if the passed exception is an {@link InterruptedException} or
     * {@link InterruptedIOException}, or if the current thread is marked interrupted.
     *
     * @param e the exception to check
     * @throws RuntimeException if there was an interrupt
     * @since 7.1
     */
    public static void checkInterrupt(Exception e) {
        if (isInterrupted(e)) {
            // reset interrupted status
            Thread.currentThread().interrupt();
            // continue interrupt
            throw new RuntimeException(e);
        }
        if (Thread.currentThread().isInterrupted()) {
            // if an InterruptedException occurred earlier but was wrapped,
            // continue interrupt
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Unwraps the exception if it's an {@link InvocationTargetException}.
     * <p>
     * Also deals with interrupts by immediately throwing an exception.
     *
     * @param e the exception to unwrap
     * @return the unwrapped exception
     * @throws RuntimeException if there was an interrupt
     * @since 7.1
     */
    public static Exception unwrapInvoke(Exception e) {
        if (e instanceof InvocationTargetException) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                // Error, throw immediately
                throw (Error) cause;
            } else if (cause instanceof Exception) {
                e = (Exception) cause;
            } else {
                // Throwable direct subclass?!
                e = new RuntimeException(cause);
            }
        }
        checkInterrupt(e);
        return e;
    }

    /**
     * Wraps the exception into a {@link RuntimeException}, if needed, for re-throw.
     * <p>
     * Deals with {@link InvocationTargetException}, {@link InterruptedException} and {@link InterruptedIOException}.
     *
     * @param e the exception to wrap
     * @return a {@link RuntimeException}
     * @throws RuntimeException if there was an interrupt
     * @since 7.1
     */
    public static RuntimeException runtimeException(Exception e) {
        e = unwrapInvoke(e);
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(e);
        }
    }

    /**
     * DON'T USE THIS METHOD - INTERNAL API.
     * <p />
     * This helper method is used to detect if an exception is caused by an {@link InterruptedException} or something
     * equivalent (for example {@link ClosedByInterruptException}. This is a temporary method, we should rely on the
     * {@link Thread#isInterrupted()} status in the future.
     * 
     * @since 9.3
     */
    public static boolean hasInterruptedCause(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (isInterrupted(t)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * @since 9.3
     */
    public static boolean isInterrupted(Throwable t) {
        return t instanceof InterruptedException || t instanceof InterruptedIOException
                || t instanceof ClosedByInterruptException;
    }

}
