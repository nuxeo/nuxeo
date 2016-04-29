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
   <script src="https://code.jquery.com/jquery-1.7.2.min.js"></script>
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
             <#if !enableDocumentationView && !Root.isEmbeddedMode() >
               <div class="tabsbutton">
                 <a href="${This.path}/doc">Add custom Documentation</a>
               </div>
             </#if>
             <div class="tabscontent">
             <#if !enableDocumentationView && !Root.isEmbeddedMode() >
               <a href="${This.path}/doc">Add custom Documentation</a>
             </#if>
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
  <footer class="site-footer" role="contentinfo">
    <div class="row5">
      <ul id="menu-footer" class="menu"><li id="menu-item-11179" class="col menu-item menu-item-type-custom menu-item-object-custom menu-item-has-children menu-item-11179"><a href="#">col</a>
        <ul class="sub-menu">
          <li id="menu-item-11184" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-11184"><a href="http://www.nuxeo.com/why-nuxeo/">Why Nuxeo?</a>
            <ul class="sub-menu">
              <li id="menu-item-11186" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11186"><a href="http://www.nuxeo.com/about/">About Us</a></li>
              <li id="menu-item-11187" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11187"><a href="http://www.nuxeo.com/careers/">Careers</a></li>
              <li id="menu-item-11188" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11188"><a href="http://www.nuxeo.com/about/leadership/#executive-team">Executive Team</a></li>
              <li id="menu-item-11189" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11189"><a href="http://www.nuxeo.com/media-center/#all">Press</a></li>
            </ul>
          </li>
          <li id="menu-item-11192" class="with_margin menu-item menu-item-type-post_type menu-item-object-page menu-item-11192"><a href="http://www.nuxeo.com/about/contact/">Contact</a></li>
          <li id="menu-item-11193" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11193"><a href="http://www.nuxeo.com/about/request-demo/">Request a Custom Demo</a></li>
        </ul>
      </li>
        <li id="menu-item-11180" class="col menu-item menu-item-type-custom menu-item-object-custom menu-item-has-children menu-item-11180"><a href="#">col</a>
          <ul class="sub-menu">
            <li id="menu-item-11194" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-11194"><a href="http://www.nuxeo.com/products/content-management-platform/">Nuxeo Platform</a>
              <ul class="sub-menu">
                <li id="menu-item-11195" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11195"><a href="http://www.nuxeo.com/products/nuxeo-live-connect/">Nuxeo Live Connect</a></li>
                <li id="menu-item-11196" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11196"><a href="http://www.nuxeo.com/products/drive-desktop-sync/">Desktop Sync</a></li>
                <li id="menu-item-11197" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11197"><a href="http://www.nuxeo.com/products/integration/">Integrations</a></li>
                <li id="menu-item-11198" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11198"><a href="http://www.nuxeo.com/elasticsearch/">Elasticsearch</a></li>
                <li id="menu-item-11199" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11199"><a href="http://www.nuxeo.com/mongodb/">MongoDB</a></li>
                <li id="menu-item-11200" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11200"><a href="http://www.nuxeo.com/rest-api/">REST API</a></li>
                <li id="menu-item-11201" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11201"><a href="http://www.nuxeo.com/?page_id=8292">CMIS</a></li>
              </ul>
            </li>
            <li id="menu-item-11202" class="with_margin menu-item menu-item-type-custom menu-item-object-custom menu-item-11202"><a href="https://connect.nuxeo.com/register/#/">Online Trial</a></li>
            <li id="menu-item-11203" class="with_margin menu-item menu-item-type-post_type menu-item-object-page menu-item-11203"><a href="http://www.nuxeo.com/products/pricing/">Pricing</a></li>
          </ul>
        </li>
        <li id="menu-item-11181" class="col menu-item menu-item-type-custom menu-item-object-custom menu-item-has-children menu-item-11181"><a href="#">col</a>
          <ul class="sub-menu">
            <li id="menu-item-11908" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-11908"><a href="http://www.nuxeo.com/solutions/digital-asset-management/">Use Cases</a>
              <ul class="sub-menu">
                <li id="menu-item-11907" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11907"><a href="http://www.nuxeo.com/solutions/digital-asset-management/">Digital Asset Management</a></li>
                <li id="menu-item-11871" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11871"><a href="http://www.nuxeo.com/solutions/case-management-government/">Case Management for Government</a></li>
                <li id="menu-item-11870" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11870"><a href="http://www.nuxeo.com/solutions/advanced-knowledge-base/">Advanced Knowledge Base</a></li>
                <li id="menu-item-11872" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11872"><a href="http://www.nuxeo.com/solutions/secure-digital-asset-delivery/">Secure Digital Asset Delivery</a></li>
                <li id="menu-item-11873" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11873"><a href="http://www.nuxeo.com/solutions/mobile-content-management/">Mobile Content Management</a></li>
              </ul>
            </li>
          </ul>
        </li>
        <li id="menu-item-11182" class="col menu-item menu-item-type-custom menu-item-object-custom menu-item-has-children menu-item-11182"><a href="#">col</a>
          <ul class="sub-menu">
            <li id="menu-item-11210" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-11210"><a href="http://www.nuxeo.com/services/">Services</a>
              <ul class="sub-menu">
                <li id="menu-item-11212" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11212"><a href="http://www.nuxeo.com/products/studio/">Nuxeo Studio</a></li>
                <li id="menu-item-11214" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11214"><a href="http://www.nuxeo.com/products/nuxeo-cloud/">Cloud Hosting</a></li>
                <li id="menu-item-11216" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11216"><a href="http://www.nuxeo.com/products/support/">Support</a></li>
                <li id="menu-item-11217" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11217"><a href="http://www.nuxeo.com/services/training/">Training</a></li>
                <li id="menu-item-11218" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-11218"><a href="http://www.nuxeo.com/services/consulting/">Consulting</a></li>
              </ul>
            </li>
            <li id="menu-item-11219" class="with_margin ppr-rewrite menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-11219"><a href="/resources">Learning</a>
              <ul class="sub-menu">
                <li id="menu-item-11221" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11221"><a href="http://www.nuxeo.com/blog">Blogs</a></li>
                <li id="menu-item-11222" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11222"><a href="http://doc.nuxeo.com/">Documentation</a></li>
                <li id="menu-item-11223" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11223"><a href="https://university.nuxeo.io/">University</a></li>
              </ul>
            </li>
            <li id="menu-item-11220" class="with_margin menu-item menu-item-type-post_type menu-item-object-page menu-item-11220"><a href="http://www.nuxeo.com/events/">Events</a></li>
          </ul>
        </li>
        <li id="menu-item-11183" class="col menu-item menu-item-type-custom menu-item-object-custom menu-item-has-children menu-item-11183"><a href="#">col</a>
          <ul class="sub-menu">
            <li id="menu-item-11226" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-11226"><a href="http://www.nuxeo.com/customers/">Customers</a>
              <ul class="sub-menu">
                <li id="menu-item-11229" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11229"><a href="http://www.nuxeo.com/customers/#aerospace">Aerospace</a></li>
                <li id="menu-item-11230" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11230"><a href="http://www.nuxeo.com/customers/#services">Business Services</a></li>
                <li id="menu-item-11231" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11231"><a href="http://www.nuxeo.com/customers/#defense-security">Defense &#038; Security</a></li>
                <li id="menu-item-11232" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11232"><a href="http://www.nuxeo.com/customers/#education-research">Education &#038; Research</a></li>
                <li id="menu-item-11233" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11233"><a href="http://www.nuxeo.com/customers/#energy">Energy</a></li>
                <li id="menu-item-11234" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11234"><a href="http://www.nuxeo.com/customers/#financial-services">Financial Services</a></li>
                <li id="menu-item-11235" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11235"><a href="http://www.nuxeo.com/customers/#government">Government</a></li>
                <li id="menu-item-11236" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11236"><a href="http://www.nuxeo.com/customers/#pharmaceutical-healthcare">Healthcare &#038; Pharmaceuticals</a></li>
                <li id="menu-item-11237" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11237"><a href="http://www.nuxeo.com/customers/#media-entertainment">Media &#038; Entertainment</a></li>
                <li id="menu-item-11238" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11238"><a href="http://www.nuxeo.com/customers/#telecom-technology">Telecom &#038; Technology</a></li>
                <li id="menu-item-11239" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-11239"><a href="http://www.nuxeo.com/customers/#travel">Travel</a></li>
              </ul>
            </li>
            <li id="menu-item-11227" class="with_margin menu-item menu-item-type-post_type menu-item-object-page menu-item-11227"><a href="http://www.nuxeo.com/partners/">Partners</a></li>
          </ul>
        </li>
      </ul>        </div>
    <div class="wrap">
      <div id="copyright">
        Copyright &copy; 2016 Nuxeo <br /> <a class="privacy" href="http://www.nuxeo.com/privacy-policy/">Privacy Policy</a>
        <ul class="icons f-right">
          <li><a href="https://www.facebook.com/Nuxeo"><img alt="facebook" title="facebook" src="http://7030-presscdn-0-54.pagely.netdna-cdn.com/wp-content/themes/nuxeo.com_wp/imgs/footer-icon-fb.png" width="30" height="30"/></a></li>
          <li><a href="http://www.linkedin.com/groups/Nuxeo-Community-43314?home=&gid=43314&trk=anet_ug_hm"><img alt="linkedin" title="linkedin" src="http://7030-presscdn-0-54.pagely.netdna-cdn.com/wp-content/themes/nuxeo.com_wp/imgs/footer-icon-ln.png" width="30" height="30"/></a></li>
          <li><a href="https://twitter.com/nuxeo"><img alt="twitter" title="twitter" src="http://7030-presscdn-0-54.pagely.netdna-cdn.com/wp-content/themes/nuxeo.com_wp/imgs/footer-icon-tw.png" width="30" height="30"/></a></li>
          <li><a href="https://plus.google.com/u/0/+nuxeo/posts"><img alt="google+" title="google+" src="http://7030-presscdn-0-54.pagely.netdna-cdn.com/wp-content/themes/nuxeo.com_wp/imgs/footer-icon-gp.png" width="30" height="30"/></a></li>
          <!--<li><a href="http://nuxeo.github.io/"><i class="fa fa-github-alt"></i></a></li>-->
        </ul>
      </div>
    </div>
    <div class="clearfix"/>
  </footer>
  </#if>
<script type="text/javascript">

    hljs.initHighlightingOnLoad();

    // toggle code viewer
    $(".resourceToggle").click(function() {
     $(this).next().toggle();
     $(this).toggleClass('resourceToggle');
     $(this).toggleClass('resourceToggleDown');
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
<#if !Root.isEmbeddedMode()>
<script type="text/javascript" src="//www.nuxeo.com/wp-content/themes/nuxeo.com_wp/js/xnav_get.js" charset="utf-8"></script>
</#if>
</body>
</html>
