/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis.retry;

import org.apache.commons.logging.LogFactory;

public class Retry {

    public interface Block<T> {
        T retry() throws FailException, ContinueException;
    }

    public interface Policy {
        boolean allow();

        void pause();
    }

    public static class ContinueException extends Exception {

        private static final long serialVersionUID = 1L;

        public ContinueException(Throwable cause) {
            super(cause);
        }

    }

    public static class FailException extends Exception {

        public FailException(String message) {
            super(message);
        }

        public FailException(Throwable cause) {
            super(cause);
        }

        private static final long serialVersionUID = 1L;

    }

    public <T> T retry(Block<T> block, Policy policy) throws FailException {
        FailException causes = new FailException("Cannot execute block, retry policy failed, check supressed exception for more infos");
        while (policy.allow()) {
            try {
                return block.retry();
            } catch (ContinueException error) {
                causes.addSuppressed(error.getCause());
            } catch (FailException error) {
                causes.addSuppressed(error.getCause());
                throw causes;
            }
            policy.pause();
        }
        throw causes;
    }

}
