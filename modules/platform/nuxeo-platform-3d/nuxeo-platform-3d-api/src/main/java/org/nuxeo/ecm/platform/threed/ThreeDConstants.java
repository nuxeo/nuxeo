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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.threed;

import java.util.ArrayList;
import java.util.List;

public class ThreeDConstants {

    public static final String THREED_TYPE = "ThreeD";

    public static final String THREED_FACET = "ThreeD";

    public static final String THREED_SCHEMA = "threed";

    public static final String THREED_CHANGED_EVENT = "threeDChanged";

    public static final String EXTENSION_COLLADA = "dae";

    public static final String EXTENSION_3DSTUDIO = "3ds";

    public static final String EXTENSION_FILMBOX = "fbx";

    public static final String EXTENSION_STANFORD = "ply";

    public static final String EXTENSION_WAVEFRONT = "obj";

    public static final String EXTENSION_EXTENSIBLE_3D_GRAPHICS = "x3d";

    public static final String EXTENSION_STEREOLITHOGRAPHY = "stl";

    public static final String EXTENSION_GLTF = "gltf";

    public static final String EXTENSION_RENDER = "png";

    public static final String THUMBNAIL_PICTURE_TITLE = "Thumbnail";

    public static final String STATIC_3D_PCTURE_TITLE = "Static3D";

    public static final List<String> SUPPORTED_EXTENSIONS = new ArrayList<String>() {
        private static final long serialVersionUID = 1L;

        {
            add(EXTENSION_COLLADA);
            add(EXTENSION_3DSTUDIO);
            add(EXTENSION_FILMBOX);
            add(EXTENSION_STANFORD);
            add(EXTENSION_WAVEFRONT);
            add(EXTENSION_EXTENSIBLE_3D_GRAPHICS);
            add(EXTENSION_STEREOLITHOGRAPHY);
            add(EXTENSION_GLTF);
        }
    };

    // Constant utility class
    private ThreeDConstants() {
    }

}
