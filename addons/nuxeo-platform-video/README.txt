====================
Nuxeo Platform Video
====================

Nuxeo addons to provide the following video features for Nuxeo document:

 - In-browser video preview with JPEG thumbnails and quicktime player
 - Storyboard extraction and navigation time navigation
 - Darwin Streaming Server (a.k.a. DSS_) integration for seeking forward large
   videos (using the storyboard for instance) without first buffering all the
   file locally.


nuxeo-platform-video-jsf
========================

Provides basic JSF templates and backing seam components to be able to display a
video player (using the quicktime plugin) that either use direct HTTP buffering
or the RTSP-based URL that plays well with a darwin streaming server instance if
a streamable version of the video is available.

This package also holds a sample templates used in DAM to display a storyboard
of a video that position the quicktime player to the right time offset when
clicking on one of the thumbnails.


nuxeo-platform-video-core
=========================


Video type definition
~~~~~~~~~~~~~~~~~~~~~

TODO


Core event listeners
~~~~~~~~~~~~~~~~~~~~

 - `VideoStoryboardListener`: compute the storyboard for document type that holds
   the `HasStoryboard` facet. The video storyboard is stored in the
   `vid:storyboard` field. Also update the `strm:duration` duration field.

 - `VideoPreviewListener`: compute the a 2 thumbnails previews (same sizes as
   the picture previews) for documents that have the `HasVideoPreview` facet.
   The results is stored in the `picture` schema using the same picture adapter
   as the `Picture` documents. If the format is not supported by ffmpeg, a black
   thumbnail is generated. Also updates the `strm:duration` field.

 - `MediaStreamingUpdaterListener`: asynch event listener to build the hinted
   streamable version of video to be used by the DSS_ integration. The results
   is stored in the `strm:streamable` field.


StreamingService and DSS_ integration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

TODO


FileManagerService contribution
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

`VideoImporter` is contributed to the `FileManagerService` to create documents
of type `Video` if the mimetype is matching when using the drag and drop plugin
or the DAM import button.


nuxeo-platform-video-convert
============================

This package holds the backend converters to compute JPEG thumbnails (preview
and storyboard) and streamable version of videos for the DSS integration.

Provides contributions to the `CommandlineExecutorService`:

 - get the ffmpeg_ output info (e.g. the duration in seconds) of a video file

 - generating a single screenshot of a video file at a given time offset in
   seconds with ffmpeg_

 - generating a sequence of regularly spaces screenshots to compute the
   storyboard of a video file with ffmpeg_

 - converting a video from any format to mp4 (container format) H264 (video
   codec) + aac (audio codec) using handbrake_ (used for streaming)

 - hinting mp4 files to make them suitable for streaming using DDS_ by using the
   MP4Box_ command.

 - checking the presence of hinting tracks in a mp4 file using mp4creator_ to
   avoid recomputing them when not necessary (optim, not used yet).

 - converting a video from any format to ogg (container format) theora (video
   codec) + vorbis (audio codec) using ffmpeg2theora_ (not used by default but
   could be use as a base for Icecast integration in the future as an
   alternative to DSS_ for instance).


All those `CommandlineExecutorService` contributions are wrapped into 3 higher
level java classes that are contributed to the `ConversionService`:

 - `ScreenshotConverter`: extract a single JPEG preview of the video

 - `StoryboardConverter`: extract a list of JPEG files with time offset info

 - `StreamableMediaConverter`: compute a streamable version of the video
   suitable for DSS_ integration.


Dependencies summary
--------------------

Here is the list of commandline programs that need to be installed in the path
of the server.


Mandatory
~~~~~~~~~

 - ffmpeg_ is needed to compute JPEG previews, storyboard, and duration
   extraction: mandatory


Mandatory if DSS_ mode enabled
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 - DSS_: the streaming server it-self

 - handbrake_ is used for encoding to h264/aac to compute the version streamable
   using darwin: only mandatory if the streaming server mode is enabled (disabled
   by default)

 - MP4Box_ is used for track hinting for mp4 files: only mandatory if the streamingi
   server mode is enabled (disabled by default)


Might be used in the future
~~~~~~~~~~~~~~~~~~~~~~~~~~~

 - mp4creator_ will be used to avoid building streamable version of videos that
   are already streamable in their original version

 - ffmpeg2theora_ is an optional dependency used by a converter that is not used
   by default in either Nuxeo DAM or Nuxeo DM


.. _ffmpeg: http://ffmpeg.org
.. _ffmpeg2theora: http://v2v.cc/~j/ffmpeg2theora/
.. _handbrake: http://handbrake.fr/
.. _MP4Box: http://gpac.sf.net
.. _mp4creator: http://mpeg4ip.sf.net
.. _DSS: http://dss.macosforge.org


