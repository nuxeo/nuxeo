<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <title>
     <@block name="title">
     Nuxeo - EasyShare
     </@block>
  </title>
  <meta name="description" content="Nuxeo Easy Share Folder">
  <meta name="viewport" content="width=device-width">
  <link href='//fonts.googleapis.com/css?family=Open+Sans' rel='stylesheet' type='text/css'>
  <link rel="stylesheet" href="${skinPath}/css/nuxeo-easyshare-embedded.css">
  <link rel="stylesheet" href="${skinPath}/css/normalize.css">
  <link rel="stylesheet" href="${skinPath}/css/site.css">
  <link rel="shortcut icon" href="${skinPath}/image/favicon.gif" />
</head>

<body>
  <nav>
    <div id="xnav">
      <div class="xnav-set clearfix">
        <ul>
          <li class=""><a href="http://www.nuxeo.com/">www.nuxeo.com</a></li>
          <li><a href="http://www.nuxeo.com/blog/">Blogs</a></li>
          <li><a href="http://answers.nuxeo.com/">Answers</a></li>
          <li><a href="http://doc.nuxeo.com/">Doc</a></li>
          <li class="selected"><a target="_blank" href="http://connect.nuxeo.com/">My Connect Account</a></li>
          <li><a href="http://www.nuxeo.com/en/about/contact">Contact us</a></li>
         </ul>
         <div id="tabby-button">
           <a title="Download the Nuxeo Platform" class="en" href="http://www.nuxeo.com/en/downloads">
             <span id="arrow">Download</span>
             <span id="more">The Nuxeo Platform</span>
           </a>
         </div>
      </div>
    </div>
  </nav>
  <div class="nuxeo-logo">
    <img src="${skinPath}/img/nuxeo_logo.png" />
  </div>

  <!--header>
    <@block name="header">Nuxeo - Easy Share  </@block>
  </header -->

  <section>
    <div class="wrapper">
      <main class="share-box">
        <@block name="content">The Content</@block>
      </main>
    </div>
  </section>
  <footer>
    <div id="xnav-footer">
        <div class="FootLinks">
          <ul>
            <li><h6>More</h6>
              <ul>
                <li><a href="http://doc.nuxeo.com/">Documentation</a>
                </li>
                <li><a href="http://www.nuxeo.com/blog/">Blogs</a>
                </li>
                <li>
                  <a href="http://answers.nuxeo.com/">Q&amp;A </a></li>
              </ul>
            </li>
          </ul>
          <ul>
            <li><h6>About Nuxeo</h6>
              <ul>
                <li><a href="http://www.nuxeo.com/">nuxeo.com</a>
                </li>
                <li><a href="http://community.nuxeo.com">Community</a>
                </li>
                <li><a href="http://www.nuxeo.com/en/about/careers">Careers</a></li>
              </ul>
            </li>
          </ul>
          <ul>
            <li><h6>Nuxeo Platform</h6>
             <ul>
              <li><a href="http://www.nuxeo.com/en/products/document-management">Document Management</a></li>
              <li><a href="http://www.nuxeo.com/en/products/social-collaboration">Social Collaboration</a></li>
              <li><a href="http://www.nuxeo.com/en/products/case-management">Case Management</a></li>
              <li><a href="http://www.nuxeo.com/en/products/digital-asset-management">Digital Asset Management</a></li>
             </ul>
            </li>
          </ul>
          <ul>
            <li><h6>Services</h6>
              <ul>
                <li><a href="http://www.nuxeo.com/en/services/connect/">Support</a></li>
                <li><a href="http://www.nuxeo.com/en/services/training">Training</a></li>
                <li><a href="http://www.nuxeo.com/en/services/consulting">Consulting</a></li>
              </ul>
            </li>
          </ul>
          <ul>
            <li><h6>Follow us</h6>
              <ul>
                <li><a onclick="_gaq.push(['_trackEvent', 'Social', 'Twitter', 'Go to Twitter page'])" href="http://twitter.com/nuxeo" rel="nofollow"><span class="twitter">Twitter</span></a></li>
                <li>
                  <a onclick="_gaq.push(['_trackEvent', 'Social', 'LinkedIn', 'Go to LinkedIn group page'])" href="http://www.linkedin.com/groups/Nuxeo-Community-43314?home=&amp;gid=43314&amp;trk=anet_ug_hm" rel="nofollow"><span class="linkedIn">LinkedIn</span></a></li>
                <li>
                  <a onclick="_gaq.push(['_trackEvent', 'Social', 'FaceBook', 'Go to FaceBook group page'])" href="https://www.facebook.com/Nuxeo" rel="nofollow"><span class="facebook">Facebook</span></a></li>
                <li>
                  <a onclick="_gaq.push(['_trackEvent', 'Social', 'GooglePlus', 'Go to FaceBook group page'])" href="https://plus.google.com/u/0/b/116828675873127390558/" rel="nofollow"><span class="facebook">Google+</span></a></li>  
              </ul>
            </li>
          </ul>
        </div>
        <div class="Contact">
          <span class="logo">nuxeo</span>
        </div>
      </div>

  </footer>
</body>
</html>
