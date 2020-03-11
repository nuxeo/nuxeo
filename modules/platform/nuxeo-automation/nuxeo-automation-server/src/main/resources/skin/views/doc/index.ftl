<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="author" content="Nuxeo">
    <meta name="description" content="Nuxeo Automation Documentation">
    <link type="text/css" rel="stylesheet" href="http://fonts.googleapis.com/css?family=PT+Sans+Caption:400,700">
    <title><#if operation?has_content>${operation.label} - </#if>Nuxeo Automation Documentation</title>
  </head>

  <style>
<#include "views/doc/style.css" />
  </style>
  <body>
    <header role="banner">
      <h1>
        <span id="logo"></span>
        <span class="doctitle">Automation Documentation</span>
      </h1>
      <nav role="complementary">
        <a href="http://answers.nuxeo.com/">Answers</a>
        <a href="http://doc.nuxeo.com">Documentation</a>
        <a href="http://connect.nuxeo.com">Nuxeo Online Services</a>
        <a href="http://www.nuxeo.com">nuxeo.com</a>
       </nav>
    </header>
    <div class="container">
      <#include "views/doc/quicknav.ftl">
    </div>
    <div class="container">
      <section>
        <article role="contentinfo">

          <#if operation?has_content>
            <#include "views/doc/operation.ftl">
          <#else>

            <h2>Operations</h2>

            <div>
              <#if browse == 'label'>
                <div class="browseStyle">
                  <a href="${This.path}?">Browse by category</a>
                  <span> - </span>
                  Browse by label
                </div>
                <div>
                  <ul>
                    <#list operations as item>
                      <li><a href="${This.path}?id=${item.id}">${item.label}</a></li>
                    </#list>
                  </ul>
                </div>
              <#else>
                <div class="browseStyle">
                  Browse by category
                  <span> - </span>
                  <a href="${This.path}?browse=label">Browse by label</a>
                </div>
                <div>
                  <#include "views/doc/menu.ftl" >
                </div>
              </#if>
            </div>

          </#if>

        </article>
      </section>
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
