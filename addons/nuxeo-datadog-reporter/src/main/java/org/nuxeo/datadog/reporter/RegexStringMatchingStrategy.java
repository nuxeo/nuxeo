/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.datadog.reporter;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

/**
 * @since 10.1
 */
class RegexStringMatchingStrategy implements StringMatchingStrategy {
    private final LoadingCache<String, Pattern> patternCache;

    RegexStringMatchingStrategy() {
        patternCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, Pattern>() {
                @Override
                public Pattern load(String regex) throws Exception {
                    return Pattern.compile(regex);
                }
            });
    }

    @Override
    public boolean containsMatch(ImmutableSet<String> matchExpressions, String metricName) {
        for (String regexExpression : matchExpressions) {
            if (patternCache.getUnchecked(regexExpression).matcher(metricName).matches()) {
                // just need to match on a single value - return as soon as we do
                return true;
            }
        }
        return false;
    }
}