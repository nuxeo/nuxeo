nuxeo-segment.io-connector
==========================

Integrate segment.io API with Nuxeo Event system

The goal is to provide : 

 - a service that Wrapps Analycis java lib provided segment.io

 - a was to configure mapping between 

     - Nuxeo events : Login, CreateDoc, WF started ...
     - segment.io calls : Identify or Track

The metadata mapping is done using a MVEL expression contributed by extension point.


