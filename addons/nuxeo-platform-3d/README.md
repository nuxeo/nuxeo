# Nuxeo Platform 3D

<img src="3D-Preview-Example.png" width="300"/>

Support for previewing 3D content with ray-tracing renders and WebGL 3D viewer.

Requirements: minimum version for Nuxeo Platform is 8.4.

Supported 3D file formats:

* Collada (.dae)
* 3D Studio (.3ds)
* FBX (.fbx)
* Stanford (.ply)
* Wavefront (.obj)
* X3D Extensible 3D (.x3d)
* Stl (.stl)

# Building

    mvn clean install

# Installation

## Server-side

### Step 1: Install bundles
Copy the 5 built artifacts into `$NUXEO_HOME/templates/custom/bundles/`

### Step 2: Install Docker
Install it directly from `https://docs.docker.com/engine/installation/`.

# How to use it
Create a new document type "3D".
Then add to the main content the (e.g.) ".obj" document and all other assets (textures, materials,..) as attached files.
it is possible to add a Zip file with all the content inside (subdirectories are allowed).

Draging and droping a 3D file (supported extension or zip with supported content) will also result in a 3D document.

Available features: 
- Preview your 3D asset
- List of downloadable transmission formats
- Render views available on the right panel

# QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/addons_nuxeo-platform-3d-master)](https://qa.nuxeo.org/jenkins/job/master/job/addons_nuxeo-platform-3d-master/)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
