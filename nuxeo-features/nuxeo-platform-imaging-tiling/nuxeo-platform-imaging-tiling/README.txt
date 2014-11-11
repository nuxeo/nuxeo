
About this package :
--------------------
The tiling service offers an API to extract pictures tiles from a blob image.

How the service works :
-----------------------
The tiling service can use several tiler program to generate the tiles.
For now Gimp and ImageMagic are supported.
Default configuration requires ImageMagick :
 - because it is faster
 - because some treatments can not be done via gimp
 - because setup is easier

Requirements :
--------------
ImageMagick 6.3.7 or later is needed : the Tiling Service uses the ImageMagick stream command that is not available in 6.2
(associated packages are libmagick and imagemagick).

Current implementation has only be tested under Linux.

This should work on any Nuxeo 5.1.4+ (tested on 5.1.6 for now).

Installation :
--------------
 - Install the requirements
 - drop the tiling-service jar into nuxeo.ear/plugins
 - restart JBoss

Configuration :
---------------
Configuration can be done using a extension point.
Just create a file called pictures-tiles-config.xml in nuxeo.ear/config

Use this extension point contrib to :
 - define the imagemagick command path (if default is not ok)
 - define the directory that will be used for cache
 - define the cachesize
 - define the GC parameters

NB : default config should be ok for any linux based system where imagemagick is setup via the package manager

<?xml version="1.0"?>
<component name="my.projects.tiles.config">
  <require>org.nuxeo.ecm.platform.pictures.tiles.default.config</require>
  <extension target="org.nuxeo.ecm.platform.pictures.tiles.service.PictureTilingComponent"
    point="environment">
  <environment>
    <parameters>
      <!-- Gimp path variables -->
      <parameter name="GimpExecutable">gimp</parameter>
      <!-- ImageMagick path variables -->
      <parameter name="IMConvert">convert</parameter>
      <parameter name="IMIdentify">identify</parameter>
      <parameter name="IMStream">stream</parameter>
      <!-- global env variables -->
      <parameter name="WorkingDirPath">/tmp/</parameter>
      <!-- Max Disk cache usage in KB -->
      <parameter name="MaxDiskSpaceUsageForCache">50000</parameter>
      <!-- GC Interval in Minutes -->
      <parameter name="GCInterval">10</parameter>
    </parameters>
  </environment>
  </extension>
</component>

Testing in Nuxeo 5 EP :
-----------------------

1 - create a simple file document with one picture
2 - get the document uuid generated in the URL of your newly created document
3 - use the felowing test URL :
http://server/nuxeo/restAPI/getTiles/default/{document_uuid}/{tileWidth}/{tileHeight}/{maxTiles}?test=true

where :
 - {document_uuid} is the uuid of the document that holds your picture
 - {tileWidth} is the width of each tile in pixels
 - {tileHeight} is the height of each tile in pixels
 - {maxTiles} is the maximum tiles you want in width/height

For example if you want 200x150 tiles and want to have the full picture displayed using at maximum a 2x2 grid on document 950b0d27-2ca4-43e4-bb12-598ad6d64e86
http://server/nuxeo/restAPI/getTiles/default/950b0d27-2ca4-43e4-bb12-598ad6d64e86/200/150/2?test=true

This URL will generate you a very basic test JS/Html UI.


Here are some other test URL :
- http://server/nuxeo/restAPI/getTiles/default/950b0d27-2ca4-43e4-bb12-598ad6d64e86/200/150/2
 will send you the tiling informations in XML format
- http://server/nuxeo/restAPI/getTiles/default/950b0d27-2ca4-43e4-bb12-598ad6d64e86/200/150/2?format=json
 same information but formated in JSON
- http://server/nuxeo/restAPI/getTiles/default/950b0d27-2ca4-43e4-bb12-598ad6d64e86/200/150/2?x=1&y=2
 will send you the tile(1,2)

