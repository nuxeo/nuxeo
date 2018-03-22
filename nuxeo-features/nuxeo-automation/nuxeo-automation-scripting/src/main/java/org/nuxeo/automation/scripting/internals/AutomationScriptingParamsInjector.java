/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.automation.scripting.internals;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;

@FunctionalInterface
public interface AutomationScriptingParamsInjector {

    public void inject(Map<String, Object> params, OperationContext ctx, ScriptingOperationDescriptor desc);

    public static AutomationScriptingParamsInjector newInstance(boolean inlinedContext) {
        if (inlinedContext) {
            return (params, ctx, desc) -> params.putAll(ctx);
        }
        return (params, ctx, desc) -> Stream.of(desc.getParams()).map(Param::getName).forEach(
                name -> Optional.ofNullable(ctx.getChainParameter(name))
                                .ifPresent(chainParameter -> params.put(name, chainParameter)));

    }

}