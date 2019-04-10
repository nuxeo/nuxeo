<!DOCTYPE html>
<html>
<head>
  <title>
    <@block name="title">Nuxeo Platform Explorer</@block>
  </title>
  <meta http-equiv="Content-Type" charset="UTF-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <meta name="author" content="Nuxeo">
  <meta name="description" content="Nuxeo Platform Explorer">

  <link rel="shortcut icon" href="${skinPath}/images/favicon.png" />
  <link rel="stylesheet" href="${skinPath}/css/apidoc_style.css" media="screen" charset="utf-8" />
  <link rel="stylesheet" href="${skinPath}/css/code.css" media="screen" charset="utf-8" />
  <link rel="stylesheet" href="${skinPath}/script/jquery//treeview/jquery.treeview.css" media="screen" charset="utf-8">
  <link rel="stylesheet" href="http://fonts.googleapis.com/css?family=PT+Sans+Caption:400,700">

  <@block name="stylesheets" />
   <script type="text/javascript">
     var skinPath = '${skinPath}';
   </script>
   <script src="${skinPath}/script/jquery/jquery.js"></script>
   <script src="${skinPath}/script/jquery/cookie.js"></script>
   <script src="${skinPath}/script/highlight.js"></script>
   <script src="${skinPath}/script/java.js"></script>
   <script src="${skinPath}/script/html-xml.js"></script>
   <script src="${skinPath}/script/manifest.js"></script>

   <script src="${skinPath}/script/jquery//treeview/jquery.treeview.js"></script>
   <script src="${skinPath}/script/jquery//treeview/jquery.treeview.async.js"></script>
   <script src="${skinPath}/script/quickEditor.js"></script>
   <script src="${skinPath}/script/jquery.highlight-3.js"></script>
   <@block name="header_scripts" />

</head>

<body>
  <#if !Root.isEmbeddedMode()>
  <header role="banner">
    <@block name="header">
    <h1>
      <span class="nuxeo">nuxeo</span><span class="slash">/</span><span class="doctitle">Platform Explorer</span>
    </h1>
    <nav role="complementary">
      <a href="http://answers.nuxeo.com/">Answers</a>
      <a href="http://doc.nuxeo.com">Documentation</a>
      <a href="http://connect.nuxeo.com">nuxeo connect</a>
      <a href="http://www.nuxeo.com">nuxeo.com</a>
      <div class="login">
         <#include "nxlogin.ftl">
         <!--input type="text" size="15" value="login">
         <input type="text" size="15" value="password">
         <input class="button" type="submit" value="ok"-->
      </div>
     </nav>
     </@block>
  </header>
  </#if>
  <div class="container">
    <table class="content">
      <tr>
      <@block name="middle">
        <#if !hideNav>
        <td class="leftblock">
          <nav role="navigation">
          <@block name="left">
          <#include "nav.ftl">
          </@block>
          </nav>
        </td>
        </#if>

        <td class="rightblock">
         <section>
           <article role="contentinfo">
           <#if enableDocumentationView && !Root.isEmbeddedMode() >
             <div class="tabsbar">
               <ul>
                 <li <#if selectedTab=="defView">class="selected"</#if> >
                   <a href="${This.path}/">View</a>
                 </li>
                 <li <#if selectedTab=="docView">class="selected"</#if> >
                   <a href="${This.path}/doc">Documentation view</a>
                 </li>
               </ul>
             </div>
             <div style="clear:both;"></div>
           </#if>
             <div class="tabscontent">
             <@block name="right">
               Content
             </@block>
             </div>
           </article>
         </section>
       </td>
      </@block>
      </tr>
    </table>
  </div>
  <#if !Root.isEmbeddedMode()>
  <footer role="contentinfo">
    <nav role="navigation">
      <ul>
       <li><h6>More</h6>
         <ul>
           <li><a href="http://doc.nuxeo.com/">Documentation</a></li>
           <li><a href="http://www.nuxeo.com/blog/">Blogs</a></li>
           <li><a href="http://answers.nuxeo.com/">Q&amp;A </a></li>
         </ul>
       </li>
     </ul>
      <ul>
       <li><h6>About Nuxeo</h6>
         <ul>
           <li><a href="http://www.nuxeo.com/">nuxeo.com</a></li>
           <li><a href="http://community.nuxeo.com">Community</a></li>
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
           <li><a onclick="_gaq.push(['_trackEvent', 'Social', 'LinkedIn', 'Go to LinkedIn group page'])" href="http://www.linkedin.com/groups/Nuxeo-Community-43314?home=&amp;gid=43314&amp;trk=anet_ug_hm" rel="nofollow"><span class="linkedIn">LinkedIn</span></a></li>
           <li><a onclick="_gaq.push(['_trackEvent', 'Social', 'FaceBook', 'Go to FaceBook group page'])" href="https://www.facebook.com/Nuxeo" rel="nofollow"><span class="facebook">Facebook</span></a></li>
           <li><a onclick="_gaq.push(['_trackEvent', 'Social', 'GooglePlus', 'Go to FaceBook group page'])" href="https://plus.google.com/u/0/b/116828675873127390558/" rel="nofollow"><span class="facebook">Google+</span></a></li>
         </ul>
       </li>
     </ul>
    </nav>
    <div class="clearfix" />
  </footer>
  </#if>
<script type="text/javascript">

    hljs.initHighlightingOnLoad();

    // toggle code viewer
    $(".resourceToggle").click(function() {
     $(this).next().toggleClass('hiddenResource');
     $(this).toggleClass('resourceToggle');
     $(this).toggleClass('resourceToggleUp');
    });

    // toggle title bars
    //$(".blocTitle").click(function() {
    // var toFold=$(this).parent().find(".foldablePanel").get(0);
    // $(toFold).toggle("fold",{horizFirst: true },10);
    //});

    var lastDisplayedDoc;
    function showAddDoc(docId) {
      if (lastDisplayedDoc) {
       if (lastDisplayedDoc!=docId) {
         $('#' + lastDisplayedDoc).toggle();
       }
      }
      $('#' + docId).toggle();
      lastDisplayedDoc=docId;
    }


</script>

<@block name="footer_scripts" />

</body>
</html>
