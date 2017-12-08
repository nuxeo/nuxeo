<!DOCTYPE html>
<html>
<head>
  <title>Nuxeo API documentation</title>
  <link type="text/css" rel="stylesheet" href="http://fonts.googleapis.com/css?family=PT+Sans+Caption:400,700">


  <link href="${skinPath}/css/highlight.default.css" media="screen" rel="stylesheet" type="text/css"/>
  <link href="${skinPath}/css/screen.css" media="screen" rel="stylesheet" type="text/css"/>
  <script type="text/javascript" src="${skinPath}/lib/shred.bundle.nx.js"></script>
  <script src="${skinPath}/lib/jquery-1.8.0.min.js" type="text/javascript"></script>
  <script src="${skinPath}/lib/jquery.slideto.min.js" type="text/javascript"></script>
  <script src="${skinPath}/lib/jquery.wiggle.min.js" type="text/javascript"></script>
  <script src="${skinPath}/lib/jquery.ba-bbq.min.js" type="text/javascript"></script>
  <script src="${skinPath}/lib/handlebars-1.0.0.js" type="text/javascript"></script>
  <script src="${skinPath}/lib/underscore-min.js" type="text/javascript"></script>
  <script src="${skinPath}/lib/backbone-min.js" type="text/javascript"></script>
  <script src="${skinPath}/lib/swagger.js" type="text/javascript"></script>
  <script src="${skinPath}/swagger-ui.js" type="text/javascript"></script>
  <script src="${skinPath}/lib/highlight.7.3.pack.js" type="text/javascript"></script>

    <script type="text/javascript">
  $(function () {




            window.swaggerUi = new SwaggerUi({
              url: "${Context.serverURL}${This.path}/resources.json",
              dom_id: "swagger-ui-container",
              supportedSubmitMethods: ['get', 'post', 'put', 'delete'],
              onComplete: function(swaggerApi, swaggerUi){
                if(console) {
                  console.log("Loaded SwaggerUI")
                }
                $('pre code').each(function(i, e) {hljs.highlightBlock(e)});
              },
              onFailure: function(data) {
                if(console) {
                  console.log("Unable to Load SwaggerUI");
                  console.log(data);
                }
              },
              docExpansion: "none"
            });


            window.swaggerUi.load();
        });

    </script>
</head>

<body>

<header role="banner">
      <h1>
        <span id="logo"></span>
        <span class="doctitle">API Documentation</span>
      </h1>
      <nav role="complementary">
        <a href="http://answers.nuxeo.com/">Answers</a>
        <a href="http://doc.nuxeo.com">Documentation</a>
        <a href="http://connect.nuxeo.com">Nuxeo Online Services</a>
        <a href="http://www.nuxeo.com">nuxeo.com</a>
       </nav>
    </header>


<div id="message-bar" class="swagger-ui-wrap">
  &nbsp;
</div>


<div id="swagger-ui-container" class="swagger-ui-wrap">

</div>





<footer role="contentinfo">
      <nav role="navigation">
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
             <li><a href="http://www.nuxeo.com/en/services/connect/">Nuxeo Online Services</a></li>
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
      </nav>
      <div class="clearfix" />
     </footer>
</body>

</html>
