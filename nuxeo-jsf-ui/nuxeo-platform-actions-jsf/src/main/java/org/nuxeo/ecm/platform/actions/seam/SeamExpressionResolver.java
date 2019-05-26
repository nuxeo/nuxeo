/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.actions.seam;

import org.jboss.seam.el.SeamELResolver;
import org.nuxeo.ecm.platform.el.ExpressionResolver;

/**
 * Seam expression resolver, adding the Seam EL resolver to the list of standard resolvers.
 *
 * @since 5.7.3
 */
public class SeamExpressionResolver extends ExpressionResolver {

    public SeamExpressionResolver() {
        super();
        add(new SeamELResolver());
    }

}
