      function $(x) {
        return document.getElementById(x);
      }

      var url= 'http://marge:7070/createrssfeed.action?types=page&types=blogpost&types=mail&types=comment&types=attachment&sort=modified&showContent=true&showDiff=true&spaces=conf_personal&rssType=rss2&maxResults=10&timeSpan=5&publicFeed=false&title=personalspacefeed&os_authType=basic';

      function showOneSection(toshow) {
        var sections = [ 'main', 'approval', 'waiting' ];
        for (var i=0; i < sections.length; ++i) {
          var s = sections[i];
          var el = $(s);
          if (s === toshow) {
            el.style.display = "block";
          } else {
            el.style.display = "none";
          }
        }
      }

      function fetchData() {
        var params = {};
        params[gadgets.io.RequestParameters.CONTENT_TYPE] =
          gadgets.io.ContentType.FEED;
        params[gadgets.io.RequestParameters.AUTHORIZATION] =
          gadgets.io.AuthorizationType.OAUTH;
        params[gadgets.io.RequestParameters.METHOD] =
          gadgets.io.MethodType.GET;
        params[gadgets.io.RequestParameters.OAUTH_SERVICE_NAME] = "";

        gadgets.io.makeRequest(url, function (response) {
          if (response.oauthApprovalUrl) {
            var onOpen = function() {
              showOneSection('waiting');
            };
            var onClose = function() {
              fetchData();
            };
            var popup = new gadgets.oauth.Popup(response.oauthApprovalUrl,
                null, onOpen, onClose);
            $('personalize').onclick = popup.createOpenerOnClick();
            $('approvaldone').onclick = popup.createApprovedOnClick();
            showOneSection('approval');
          } else if (response.data) {
        	  var html='';
              html += "<ul>\n";
              var feed = response.data;
              for (var counter = 0; counter < feed.Entry.length; counter++)  {
                  html += "<li>" + '<a href="' +
                  feed.Entry[counter].Link + '" target="_blank">' + 
                  feed.Entry[counter].Title + "</a>"  + "</li>";
              }
              html += "</ul>\n";
              $('main').innerHTML=html;
              showOneSection('main');
              gadgets.window.adjustHeight();
          } else {
            var whoops = document.createTextNode(
                'OAuth error: ' + response.oauthError + ': ' +
                response.oauthErrorText);
            $('main').appendChild(whoops);
            showOneSection('main');
          }
        }, params);
      }

      gadgets.util.registerOnLoadHandler(fetchData);