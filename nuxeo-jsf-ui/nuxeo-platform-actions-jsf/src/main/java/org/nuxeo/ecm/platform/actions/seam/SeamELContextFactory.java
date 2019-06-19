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
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.actions.seam;

import javax.el.ELContext;

import org.nuxeo.ecm.platform.el.ELContextFactory;

/**
 * Factory for an ELContext that can resolve the Seam context.
 *
 * @since 8.3
 */
public class SeamELContextFactory implements ELContextFactory {

    @Override
    public ELContext get() {
        return SeamActionContext.createELContext();
    }

}
