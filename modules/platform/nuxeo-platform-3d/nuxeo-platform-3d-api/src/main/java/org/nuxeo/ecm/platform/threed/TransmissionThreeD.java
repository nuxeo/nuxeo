/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed;

import org.nuxeo.ecm.core.api.Blob;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Object wrapping a transmission format with {@link ThreeD} and LoD details
 *
 * @since 8.4
 */
public class TransmissionThreeD extends ThreeD {

    public static final String NAME = "name";

    public static final String CONTENT = "content";

    public static final String PERC_POLY = "percPoly";

    public static final String MAX_POLY = "maxPoly";

    public static final String PERC_TEX = "percTex";

    public static final String MAX_TEX = "maxTex";

    public static final String INFO = "info";

    protected final Integer percPoly;

    protected final Long maxPoly;

    protected final Integer percTex;

    protected final String maxTex;

    protected final String name;

    public TransmissionThreeD(Blob blob, List<Blob> resources, ThreeDInfo info, Integer percPoly, Long maxPoly,
            Integer percTex, String maxTex, String name) {
        super(blob, resources, info);
        this.percPoly = percPoly;
        this.maxPoly = maxPoly;
        this.percTex = percTex;
        this.maxTex = maxTex;
        this.name = name;
    }

    public TransmissionThreeD(Map<String, Serializable> map) {
        super((Blob) map.get(CONTENT), null,
                (map.get(INFO) != null) ? new ThreeDInfo((Map<String, Serializable>) map.get(INFO)) : null);
        name = (String) map.get(NAME);
        Long percPolyLong = (Long) map.get(PERC_POLY);
        percPoly = (percPolyLong != null) ? percPolyLong.intValue() : null;
        Long maxPolyLong = (Long) map.get(MAX_POLY);
        maxPoly = (maxPolyLong != null) ? maxPolyLong : null;
        Long percTexLong = (Long) map.get(PERC_POLY);
        percTex = (percTexLong != null) ? percTexLong.intValue() : null;
        maxTex = (String) map.get(MAX_TEX);
    }

    public String getTitle() {
        return name;
    }

    public Integer getPercPoly() {
        return percPoly;
    }

    public Long getMaxPoly() {
        return maxPoly;
    }

    public Integer getPercTex() {
        return percTex;
    }

    public String getMaxTex() {
        return maxTex;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return String.valueOf(name.hashCode() & 0x7fffffff);
    }

    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<>();
        map.put(NAME, name);
        map.put(CONTENT, (Serializable) blob);
        map.put(PERC_POLY, percPoly);
        map.put(MAX_POLY, maxPoly);
        map.put(PERC_TEX, percTex);
        map.put(MAX_TEX, maxTex);
        map.put(INFO, (info != null) ? (Serializable) info.toMap() : null);
        return map;
    }
}
