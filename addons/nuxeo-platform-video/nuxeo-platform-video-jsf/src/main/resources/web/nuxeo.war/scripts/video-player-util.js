NXVideo = {};

NXVideo.initializeVideoThumbnails = function(options) {
  var videoJsElement = jQuery(".video-js");
  if (videoJsElement.length > 0 && videoJsElement[0].player !== 'undefined') {
    var videoPlayer = videoJsElement[0].player;
    videoPlayer.thumbnails(options);
  }
};

NXVideo.initializeStoryBoard = function() {
  var videoJsElement = jQuery(".video-js");
  if (videoJsElement.length > 0 && videoJsElement[0].player !== 'undefined') {
    var videoPlayer = videoJsElement[0].player;
    jQuery(".videoStoryboardItem").css('cursor', 'pointer');
    jQuery(".videoStoryboardItem").click(function() {
      videoPlayer.currentTime(parseFloat(jQuery(this).attr('timecode')));
      videoPlayer.play();
      return false;
    });
  }
};
