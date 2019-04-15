# Nuxeo Platform scripts

* fixeclipse.py: Fix eclipse classpath & .ok files
* gitf.bat/gitfunctions.sh: Convenient functions for use on Nuxeo projects version controlled under Git
* release.py: Manages releasing of Nuxeo source code
* release_mp.py: Manages releasing of Nuxeo Marketplace packages

# Installation
 - (Optional) Create a Python virtalenv
 - Install requirements with `python -m pip install -r requirements.txt`
# Usage
`./<script> <options>`

* Call `fixeclipse` after an execution of `mvn eclipse:eclipse`
* Use `gitf` (or `gitfa`) to execute recursive Git actions on Nuxeo repositories (including addons)
* See `release.py -h` and `release_mp.py -h`

# Code
## QA

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/nuxeo-master)](https://qa.nuxeo.org/jenkins/job/master/job/nuxeo-master)

## Running Unit Tests
Follow the Installation steps to acquire the dependencies
Run:
    nuxeo$ cd scripts
    nuxeo/scripts$ python -m pytest

# Contributing / Reporting issues
https://jira.nuxeo.com/secure/CreateIssue!default.jspa?project=NXBT

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

# About Nuxeo

The [Nuxeo Platform](http://www.nuxeo.com/products/content-management-platform/) is an open source customizable and extensible content management platform for building business applications. It provides the foundation for developing [document management](http://www.nuxeo.com/solutions/document-management/), [digital asset management](http://www.nuxeo.com/solutions/digital-asset-management/), [case management application](http://www.nuxeo.com/solutions/case-management/) and [knowledge management](http://www.nuxeo.com/solutions/advanced-knowledge-base/). You can easily add features using ready-to-use addons or by extending the platform using its extension point system.

The Nuxeo Platform is developed and supported by Nuxeo, with contributions from the community.

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).
