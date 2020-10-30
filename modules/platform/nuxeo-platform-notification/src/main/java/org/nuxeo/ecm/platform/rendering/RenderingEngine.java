/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dragos
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.rendering;

/**
 * A RenderingEngine will be instantiated by the RenderingService according with the descriptor specified for it. The
 * specific implementation of a RenderingEngine must be in classpath for it to be instantiated and used.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface RenderingEngine {

    String getFormatName();

    /**
     * Processes the given context and return a rendering result.
     * <p>
     * The processing must never return null. If some error occurs it must throw an exception.
     *
     * @param ctx the context to process
     */
    RenderingResult process(RenderingContext ctx) throws RenderingException;
}
