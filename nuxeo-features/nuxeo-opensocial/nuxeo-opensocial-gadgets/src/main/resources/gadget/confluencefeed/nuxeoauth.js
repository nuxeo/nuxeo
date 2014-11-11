      function $(x) {
        return document.getElementById(x);
      }

      //var url= 'http://127.0.0.1:9090/createrssfeed.action?types=page&types=blogpost&types=mail&types=comment&types=attachment&sort=modified&showContent=true&showDiff=true&spaces=conf_personal&rssType=rss2&maxResults=10&timeSpan=5&publicFeed=false&title=personalspacefeed&os_authType=basic';
      var url= 'http://127.0.0.1:9090/createrssfeed.action?types=page&pageSubTypes=comment&pageSubTypes=attachment&types=blogpost&blogpostSubTypes=comment&blogpostSubTypes=attachment&types=mail&spaces=conf_all&title=Confluence+RSS+Feed&sort=modified&maxResults=10&timeSpan=5&showContent=true&confirm=Create+RSS+Feed&showDiff=false&os_authType=basic';

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

      function showSummary(id) {
        document.getElementById('summary' + id).style.display='block';
      }

      function fetchData() {
        var params = {};
        params[gadgets.io.RequestParameters.CONTENT_TYPE] =
          gadgets.io.ContentType.FEED;
        params[gadgets.io.RequestParameters.AUTHORIZATION] =
          gadgets.io.AuthorizationType.OAUTH;
        params[gadgets.io.RequestParameters.METHOD] =
          gadgets.io.MethodType.GET;
        params[gadgets.io.RequestParameters.GET_SUMMARIES] = true;
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
              html += "<div>";
              var feed = response.data;
              for (var counter = 0; counter < feed.Entry.length; counter++)  {
                  html += "<div>";
                  html += '<a href="';
                  html += feed.Entry[counter].Link + '" target="_blank"><h3>';
                  html += "<img src='http://127.0.0.1:9090/images/icons/docs_16.gif'/> &nbsp;";
                  html += feed.Entry[counter].Title + "</h3></a> &nbsp;";
                  var milliseconds = (feed.Entry[counter].Date) * 1000;
                  var date = new Date(milliseconds);
                  html += "<span class='date'>"
                  html += date.toLocaleDateString();
                  html += " ";
                  html += date.toLocaleTimeString();
                  html += "</span>"
                  html += "<br><A href=\"javascript:showSummary('" + counter + "')\">more</A>";
                  html += "<span  style='display:none' id='summary" + counter + "'>" + feed.Entry[counter].Summary + "</span>";
                  html += "</div><hr/>";
              }
              html += "</div>";
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