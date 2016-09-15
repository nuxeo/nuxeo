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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Object containing info about a 3D content
 *
 * @since 8.4
 */
public class ThreeDInfo {

    private static final long serialVersionUID = 1L;

    public static final String NON_MANIFOLD_VERTICES = "non_manifold_vertices";

    public static final String NON_MANIFOLD_EDGES = "non_manifold_edges";

    public static final String NON_MANIFOLD_POLYGONS = "non_manifold_polygons";

    public static final String VERTICES = "vertices";

    public static final String EDGES = "edges";

    public static final String POLYGONS = "polygons";

    public static final String POSITION_X = "position_x";

    public static final String POSITION_Y = "position_y";

    public static final String POSITION_Z = "position_z";

    public static final String DIMENSION_X = "dimension_x";

    public static final String DIMENSION_Y = "dimension_x";

    public static final String DIMENSION_Z = "dimension_x";

    public static final String TEXTURES_SIZE = "textures_size";

    public static final String TEXTURES_MAX_DIMENSION = "textures_max_dimension";

    public final Long nonManifoldVertices;

    public final Long nonManifoldEdges;

    public final Long nonManifoldPolygons;

    public final Long vertices;

    public final Long edges;

    public final Long polygons;

    public final Float positionX;

    public final Float positionY;

    public final Float positionZ;

    public final Float dimensionX;

    public final Float dimensionY;

    public final Float dimensionZ;

    public final Float texturesSize;

    public final String texturesMaxDimension;

    public ThreeDInfo(Map<String, Serializable> map) {
        nonManifoldVertices = (Long) map.get(NON_MANIFOLD_VERTICES);
        nonManifoldEdges = (Long) map.get(NON_MANIFOLD_EDGES);
        nonManifoldPolygons = (Long) map.get(NON_MANIFOLD_POLYGONS);
        vertices = (Long) map.get(VERTICES);
        edges = (Long) map.get(EDGES);
        polygons = (Long) map.get(POLYGONS);
        positionX = (Float) map.get(POSITION_X);
        positionY = (Float) map.get(POSITION_Y);
        positionZ = (Float) map.get(POSITION_Z);
        dimensionX = (Float) map.get(DIMENSION_X);
        dimensionY = (Float) map.get(DIMENSION_Y);
        dimensionZ = (Float) map.get(DIMENSION_Z);
        texturesSize = (Float) map.get(TEXTURES_SIZE);
        texturesMaxDimension = (String) map.get(TEXTURES_MAX_DIMENSION);
    }

    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<>();
        map.put(NON_MANIFOLD_VERTICES, nonManifoldVertices);
        map.put(NON_MANIFOLD_EDGES, nonManifoldEdges);
        map.put(NON_MANIFOLD_POLYGONS, nonManifoldPolygons);
        map.put(VERTICES, vertices);
        map.put(EDGES, edges);
        map.put(POLYGONS, polygons);
        map.put(POSITION_X, positionX);
        map.put(POSITION_Y, positionY);
        map.put(POSITION_Z, POSITION_Z);
        map.put(DIMENSION_X, dimensionX);
        map.put(DIMENSION_Y, dimensionY);
        map.put(DIMENSION_Z, dimensionZ);
        map.put(TEXTURES_SIZE, texturesSize);
        map.put(TEXTURES_MAX_DIMENSION, texturesMaxDimension);
        return map;
    }

}
