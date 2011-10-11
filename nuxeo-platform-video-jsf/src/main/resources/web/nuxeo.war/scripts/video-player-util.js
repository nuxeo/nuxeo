(function() {
  jQuery(document).ready(function() {
    jQuery(".video-js").VideoJS();
    var videoPlayer = jQuery(".video-js")[0].player;

    jQuery(".videoStoryboardItem").click(function() {
      videoPlayer.currentTime(parseFloat(jQuery(this).attr('timecode')));
      videoPlayer.play();
      return false;
    })
  });
});
