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
 *     pierre
 */
package org.nuxeo.ecm.platform.picture.recompute;

import static org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction.PARAM_XPATH;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;

/**
 * @since 11.1
 */
public class RecomputeViewsActionValidation extends AbstractBulkActionValidation {

    @Override
    protected List<String> getParametersToValidate() {
        return Collections.singletonList(PARAM_XPATH);
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {
        String xpath = command.getParam(PARAM_XPATH);
        validateXpath(PARAM_XPATH, xpath, command);
    }

}
