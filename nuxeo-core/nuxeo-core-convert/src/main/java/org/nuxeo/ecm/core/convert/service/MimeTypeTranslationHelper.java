/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.convert.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * Helper class to manage chains of converters.
 *
 * @author tiry
 */
public class MimeTypeTranslationHelper {

    private static final Logger log = LogManager.getLogger(MimeTypeTranslationHelper.class);

    /**
     * @since 10.3
     */
    public static final String ANY_MIME_TYPE = "*";

    /**
     * @since 10.3
     */
    public static final Pattern MIME_TYPE_PATTERN = Pattern.compile("(.*?);(.*)", Pattern.DOTALL);

    protected final Map<String, List<ConvertOption>> srcMappings = new HashMap<>();

    protected final Map<String, List<ConvertOption>> dstMappings = new HashMap<>();

    public void addConverter(ConverterDescriptor desc) {
        List<String> sMts = desc.getSourceMimeTypes();
        String dMt = desc.getDestinationMimeType();

        List<ConvertOption> dco = dstMappings.computeIfAbsent(dMt, key -> new ArrayList<>());
        for (String sMT : sMts) {
            List<ConvertOption> sco = srcMappings.computeIfAbsent(sMT, key -> new ArrayList<>());
            sco.add(new ConvertOption(desc.getConverterName(), dMt));
            dco.add(new ConvertOption(desc.getConverterName(), sMT));
        }
        log.debug("Added converter {} to {}", desc::getSourceMimeTypes, desc::getDestinationMimeType);
    }

    /**
     * Returns the last registered converter name for the given {@code sourceMimeType} and {@code destinationMimeType}.
     * <p>
     * Follows the algorithm of {@link #getConverterNames(String, String)}.
     *
     * @see #getConverterNames(String, String)
     * @see #getConverterName(String, String, boolean)
     */
    public String getConverterName(String sourceMimeType, String destinationMimeType) {
        return getConverterName(sourceMimeType, destinationMimeType, true);
    }

    /**
     * Returns the last registered converter name for the given {@code sourceMimeType} and {@code destinationMimeType}.
     * <p>
     * Follows the algorithm of {@link #getConverterNames(String, String, boolean)}.
     *
     * @since 11.1
     * @see #getConverterNames(String, String, boolean)
     */
    public String getConverterName(String sourceMimeType, String destinationMimeType, boolean allowWildcard) {
        List<String> converterNames = getConverterNames(sourceMimeType, destinationMimeType, allowWildcard);
        return converterNames.isEmpty() ? null : converterNames.get(converterNames.size() - 1);
    }

    /**
     * Returns {@code true} if the given {@code mimeTypes} has a compatible mime type with {@code mimeType},
     * {@code false} otherwise.
     * <p>
     * The {@code mimeTypes} list has a compatible mime type if:
     * <ul>
     * <li>it contains "*"</li>
     * <li>it contains exactly {@code mimeType}</li>
     * <li>it contains a mime type with the same primary type as {@code mimeType} and a wildcard sub type</li>
     * </ul>
     *
     * @since 10.3
     */
    public boolean hasCompatibleMimeType(List<String> mimeTypes, String mimeType) {
        String mt = parseMimeType(mimeType);
        Set<String> expectedMimeTypes = new HashSet<>();
        expectedMimeTypes.add(ANY_MIME_TYPE);
        if (mt != null) {
            expectedMimeTypes.add(mt);
            expectedMimeTypes.add(computeMimeTypeWithWildcardSubType(mt));
        }
        return mimeTypes.stream().anyMatch(expectedMimeTypes::contains);
    }

    /**
     * Returns the list of converter names handling the given {@code sourceMimeType} and {@code destinationMimeType}.
     *
     * @see #getConverterNames(String, String, boolean)
     */
    public List<String> getConverterNames(String sourceMimeType, String destinationMimeType) {
        return getConverterNames(sourceMimeType, destinationMimeType, true);
    }

    /**
     * Returns the list of converter names handling the given {@code sourceMimeType} and {@code destinationMimeType}.
     * <p>
     * Finds the converter names based on the following algorithm:
     * <ul>
     * <li>Find the converters exactly matching the given {@code sourceMimeType} and matching the given {@code
     * destinationMimeType}</li>
     * <li>If no converter found, find the converters matching a wildcard subtype based on the {@code sourceMimeType},
     * such has "image/*", and matching the given {@code destinationMimeType}</li>
     * <li>If no converter found and {@code allowWildcard} is {@code true}, find the converters matching a wildcard
     * source mime type "*" and matching the given {@code destinationMimeType}</li>
     * </ul>
     *
     * @param allowWildcard {@code true} to allow returning converters with '*' as source mime type.
     * @since 11.1
     */
    public List<String> getConverterNames(String sourceMimeType, String destinationMimeType, boolean allowWildcard) {
        // remove content type parameters if any
        String srcMimeType = parseMimeType(sourceMimeType);

        List<String> converterNames = doGetConverterNames(srcMimeType, destinationMimeType);
        if (converterNames.isEmpty()) {
            // use a mime type with a wildcard sub type
            converterNames = doGetConverterNames(computeMimeTypeWithWildcardSubType(srcMimeType), destinationMimeType);
        }

        if (converterNames.isEmpty() && allowWildcard) {
            // use a wildcard mime type
            converterNames = doGetConverterNames(ANY_MIME_TYPE, destinationMimeType);
        }

        return converterNames;
    }

    /**
     * Parses the given {@code mimeType} and returns only the primary type and optionally the sub type if any.
     * <p>
     * Some input/output samples:
     * <ul>
     * <li>"image/jpeg" => "image/jpeg"</li>
     * <li>"image/*" => "image/*"</li>
     * <li>"image/png; param1=foo; param2=bar" => "image/png"</li>
     * </ul>
     *
     * @since 10.3
     */
    protected String parseMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }

        return MIME_TYPE_PATTERN.matcher(mimeType).replaceAll("$1").trim();
    }

    /**
     * Returns a new mime type with the primary type of the given {@code mimeType} and a wildcard sub type.
     * <p>
     * Some input/output samples:
     * <ul>
     * <li>"image/jpeg" => "image/*"</li>
     * <li>"video/*" => "video/*"</li>
     * <li>"application/pdf" => "application/*"</li>
     * </ul>
     *
     * @since 10.3
     */
    protected String computeMimeTypeWithWildcardSubType(String mimeType) {
        return mimeType != null ? mimeType.replaceAll("(.*)/(.*)", "$1/" + ANY_MIME_TYPE) : null;
    }

    /**
     * Returns the list of converter names matching exactly the given {@code sourceMimeType} and {@code
     * destinationMimeType}.
     *
     * @since 11.5
     */
    protected List<String> doGetConverterNames(String sourceMimeType, String destinationMimeType) {
        return srcMappings.getOrDefault(sourceMimeType, Collections.emptyList())
                          .stream()
                          .filter(co -> destinationMimeType == null || destinationMimeType.equals(co.mimeType))
                          .map(co -> co.converter)
                          .collect(Collectors.toList());
    }

    /**
     * @deprecated since 10.3. Not used.
     */
    @Deprecated
    public List<String> getDestinationMimeTypes(String sourceMimeType) {
        List<String> dst = new ArrayList<>();

        List<ConvertOption> sco = srcMappings.get(sourceMimeType);

        if (sco != null) {
            for (ConvertOption co : sco) {
                dst.add(co.getMimeType());
            }
        }
        return dst;
    }

    /**
     * @deprecated since 10.3. Not used.
     */
    @Deprecated
    public List<String> getSourceMimeTypes(String destinationMimeType) {
        List<String> src = new ArrayList<>();

        List<ConvertOption> dco = dstMappings.get(destinationMimeType);

        if (dco != null) {
            for (ConvertOption co : dco) {
                src.add(co.getMimeType());
            }
        }
        return src;
    }

    public void clear() {
        dstMappings.clear();
        srcMappings.clear();
        log.debug("clear");
    }

}
