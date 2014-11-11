// ************************************************
// Simple JQuery plugin to enable Drag out of files

(function($) {
   $.fn.dropout = function ( baseUrl , repo ) {
      for (i=0; i< this.length; i++) {
          var target = $(this[i]);
          var id = target.attr("id");
          var docRef = target.attr("docRef");
          if (docRef==null) {
          var url = target.attr("downloadUrl");
          var mt = target.attr("mimetype");
          var filename = target.attr("filename");
          if (url!=null && mt!=null && filename!=null) {
            target.attr("draggable", "true");
            target.bind("dragstart", getDirectDownloadInfo(url,mt,filename));
            } else {
              //console.log("no download info found!!!");
            }
          } else {
            var url = baseUrl + "nxdownloadinfo/" + repo + "/" + docRef;
            target.attr("draggable", "true");
            target.bind("dragstart", getDownloadInfoFetcher(url));
          }
        }
   }

   // using to function returning a function to force closure !
   function  getDirectDownloadInfo(url,mt,filename) {
       var downloadURL = mt + ":" + filename + ":" + url;
       return function(event) {
             event.originalEvent.dataTransfer.setData("DownloadURL", downloadURL);
       };
     }

   function  getDownloadInfoFetcher(url) {
     return function(event) {
           //console.log("dropout");
           jQuery.ajax({
             async: false,
             complete: function(data) {
               event.originalEvent.dataTransfer.setData("DownloadURL", data.responseText);
               },
             error: function(xhr) {
               if (xhr.status == 404) {
                 xhr.abort();
               }
             },
             type: 'GET',
             timeout: 3000,
             url: url
           });

           event.stopPropagation();
           };
   }
})(jQuery);


