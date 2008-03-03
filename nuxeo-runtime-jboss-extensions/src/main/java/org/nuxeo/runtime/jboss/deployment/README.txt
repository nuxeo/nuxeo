This package contains a custom EAR deployer for JBoss featuring preprocessing.

Bundle-Category


bundle.project:bundle.server:bundle.classifier:bundle.type
==========================
project : server : classifier [: type]
==========================

project     :=  nuxeo | nuxeo.areva etc.
server      := common, runtime, framework | core | platform | async | web | rcp
classifier  := workflow.document.api ===> extension is important - ex: plugin, listnener, api or impl.
*** plugin = default
type        : = java | jar | ejb | rar | etc.

nuxeo:framework:runtime.scripting:java 
nuxeo:async:platform.search:ejb
nuxeo:async:platform.search
    

a