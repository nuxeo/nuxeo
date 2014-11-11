<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="author" content="Nuxeo">
    <meta name="description" content="Nuxeo Automation Documentation">
    <link type="text/css" rel="stylesheet" href="http://fonts.googleapis.com/css?family=PT+Sans+Caption:400,700">
    <title>Nuxeo Automation Documentation</title>
  </head>
  <style>
  article, aside, details, figcaption, figure, footer, header, hgroup, nav, section {
    display: block;}
  html {
    font-size: 100%;
    overflow-y: scroll;
    -webkit-text-size-adjust: 100%;
    -ms-text-size-adjust: 100%;}
  body {
    background-color: #e8eff5;
    color: #444;
    margin: 0;
    font: 1em/1.4em 'PT Sans Caption', Helvetica, Arial, sans-serif;
    line-height: 1.4;}
  button, input, select, textarea {
    font-family: sans-serif;
    color: #333332;}
  ::-moz-selection {
    background: #0076C6;
    color: #fff;
    text-shadow: none;}
  ::selection {
    background: #0076C6;
    color: #fff;
    text-shadow: none;}
  a,
  a:visited {
    color: #06bbe5;
    text-decoration: none;
    transition: all 0.15s ease 0s;}
  a:hover {
    color: #006ab1;}
  a:focus {
    outline: thin dotted;}
  a:hover, a:active {
    outline: 0;}
  dfn {
    font-style: italic;}
  pre, code, kbd, samp {
    font-family: monospace, serif;
    _font-family: 'courier new', monospace;
    font-size: 1em;}
  pre {
    white-space: pre;
    white-space: pre-wrap;
    word-wrap: break-word;}
  small {
    font-size: 85%;}
  ul, ol {
    margin: 1em 0;
    padding: 0 0 0 40px;}
  nav ul, nav ol {
    list-style: none;
    list-style-image: none;
    margin: 0;
    padding: 0;}
  img {
    border: 0;
    -ms-interpolation-mode: bicubic;
    vertical-align: middle;
    max-width: 100%;}
  form,
  figure {
    margin: 0; }
  button, input, select, textarea {
    font-size: 100%;
    margin: 0;
    vertical-align: baseline;
    *vertical-align: middle;}
  button, input {
    line-height: normal;}
  button::-moz-focus-inner,
  input::-moz-focus-inner {
    border: 0;
    padding: 0;}
  textarea {
    overflow: auto;
    vertical-align: top;
    resize: vertical;}
  table {
    border: 1px solid #dedede;
    border-collapse: collapse;
    border-spacing: 0;}
  td, th {
    padding: .4em;
    vertical-align: top;
    border: 1px solid #eee;}
  pre {
    background: none repeat scroll 0 0 #f0f4f8;
    border: 1px dashed #ccc;
    color: #09a1c5;
    font-size: .9em;
    margin: .8em;
    max-width: 100%;
    overflow: auto;
    padding: .7em;
    white-space: pre;}

  header[role="banner"]{
    background-color: #1b5983;
    color: #fff;
    font-family: sans-serif;
    vertical-align: middle;}
    header span {
      margin-right: .15em; }
    header .slash {
      color: #ffde00;}
    header .slash,
    header .doctitle {
      font-weight: 100;}
    header > nav {
      float: right;
      font-size: .8em;
      margin: 0 1.5em;}
    header a,
    header a:visited {
      border-radius: 0 0 3px 3px;
      color: rgba(255, 255, 255, 0.4);
      display: inline-block;
      margin: 0 .1em;
      padding: 1.3em .4em .2em;}
    header a:hover {
      color: #06bbe5;}
  .container {
    margin: 2em auto;
    max-width: 1000px;
    min-height: 100%;
    position: relative;
    width: 80%;}
  .container a:hover {
    background-color: #f0f4f8;}
  article {
    background-color: #fff;
    border-bottom: 3px solid #d3e0eb;
    font-size: .95em;
    margin: 0 0 0 20em;
    padding: 1em 1.5em 1.5em;}
  .container nav {
    float: left;
    font-size: .75em;
    margin: 0 -18em 4em 0;
    padding-top: 0;
    position: relative;
    width: 28%;}
    .container nav a:hover {
      margin-left: .4em;}
    .container nav .index {
      display: inline-block;
      margin-bottom: .4em;}
    .container nav .category {
      border-bottom: 1px solid;
      color: #3670b9;
      font-size: 1.2em;
      margin-bottom: .3em;}
    nav ol {
      padding-bottom: 0.5em;}
  h1 {
    font-family: helvetica;
    font-size: 1.4em;
    display: inline-block;
    letter-spacing: .03em;
    margin: 0;
    padding: .4em 1em;}
  h2 {
    color:#000;
    margin: 0 0 1em;}
  h3 {
    color:#3670b9;
    margin: 1.7em 0 0.7em;}
  .sticker {
    background-color: #f0f4f8;
    border: 1px solid #dedede;
    border-radius: 3px;
    color: #000;
    display: inline-block;
    font-size: .8em;
    font-weight: bold;
    padding: 0 .3em;
    text-transform: uppercase;}
  .sticker-deprecated {
    background-color: #fffbca;
    border-color: #efe257;}
  .sticker-studio {
    background-color: #dfffc5;
    border-color: #bbf18f;}
  footer {
    background-color: #3670b9;
    box-shadow: 0 6px 5px rgba(0, 0, 0, 0.2) inset;
    clear: both;
    color: #f1f2f3;
    font: 12px 'Lucida Grande',Lucida,Arial,Helvetica,Sans-serif;
    margin: 0;
    padding: 10px 0;
    text-align: center;}
    footer nav {
      margin: 0 auto 10px;
      max-width: 1000px;}
    footer nav ul {
      float: left;
      list-style: none outside none;
      margin: 0;
      padding: 5px 20px;
      text-align: left;}
    footer nav li {
      line-height: 150%;
      list-style: none outside none;
      margin: 0 0 0 25px;
      padding: 0 0 2px;
      text-align: left;}
    footer h6 {
      color: #f1f2f3;
      font-size: 12px;
      margin: 0;
      padding: 16px 0 8px;
      text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.5);}
    footer ul ul {
      list-style: none outside none;
      margin: 5px 0;
      padding: 0;}
    footer ul li ul li {
      list-style-type: disc;
      text-transform: none;}
    footer a:link,
    footer a:visited,
    footer a:active,
    footer a.PagerLink {
      color: #f1f2f3;
      font-size: 11px;
      text-decoration: none;}
    footer a:hover {
      color: #fff;}
    .clearfix {
      clear: both;}
  </style>
  <body>
    <header role="banner">
      <h1>
        <span class="nuxeo">nuxeo</span><span class="slash">/</span><span class="doctitle">Automation Documentation</span>
      </h1>
      <nav role="complementary">
        <a href="http://answers.nuxeo.com/">Answers</a>
        <a href="http://doc.nuxeo.com">Documentation</a>
        <a href="http://connect.nuxeo.com">nuxeo connect</a>
        <a href="http://www.nuxeo.com">nuxeo.com</a>
       </nav>
    </header>
    <div class="container">
      <nav role="navigation">
        <ul>
          <a href="?" class="index">View all operations</a>
          <#list categories?keys as cat>
          <li class="category">${cat?xml}</li>
          <ol class="category_content">
            <#list categories["${cat}"] as item>
            <li class="item"><a href="?id=${item.id}">${item.label}</a></li>
            </#list>
          </ol>
          </#list>
        </ul>
      </nav>
      <section>
        <article role="contentinfo">
          <#if operation?has_content>
            <h2>${operation.label}</h2>
            <div class="description">
              ${operation.description}
            </div>

            <h3>General Information</h3>
            <div class="info">
              <div><span class="sticker sticker-studio">Exposed in Studio</span> <#if operation.addToStudio>Yes</#if><#if !operation.addToStudio>No</#if></div>
              <div><span class="sticker">Category</span> ${operation.category?xml}</div>
              <div><span class="sticker">Operation Id</span> ${operation.id}</div>
              <div><span class="sticker">Operation Class</span> ${operation.implementationClass}</div>
              <#if operation.since?has_content>
                <div><span class="sticker">Available Since</span> ${operation.since}</div>
              </#if>
              <#if operation.deprecatedSince?has_content>
                <div><span class="sticker sticker-deprecated">Deprecated Since</span> ${operation.deprecatedSince}</div>
              </#if>
            </div>

            <h3>Parameters</h3>
            <div class="params">
              <table width="100%">
                <tr align="left">
                  <th>Name</th>
                  <th>Description</th>
                  <th>Type</th>
                  <th>Required</th>
                  <th>Default value</th>
                </tr>
                <#list operation.params as para>
                <tr>
                  <td><#if para.isRequired()><b></#if>${para.name}<#if para.isRequired()><b></#if></td>
                  <td>${para.description}</td>
                  <td>${para.type}</td>
                  <td><#if para.isRequired()>true<#else>false</#if></td>
                  <td>${This.getParamDefaultValue(para)}&nbsp;</td>
                </tr>
                </#list>
              </table>
            </div>

            <#if This.hasOperation(operation)>
            <h2>Operations</h2>
            <div class="signature">
              <ul>
                <#list operation.getOperations() as operation>
                <li><a href="?id=${operation.getId()}">${operation.getId()}</a></li>
                </#list>
              </ul>
            </div>
            </#if>

            <h3>Signature</h3>
            <div class="signature">
              <div><span class="sticker">Inputs</span> ${This.getInputsAsString(operation)}</div>
              <div><span class="sticker">Outputs</span> ${This.getOutputsAsString(operation)}</div>
            </div>

            <h3>Links</h3>
            <div><a href="${operation.id}">JSON definition</a></div>

            <h3>Traces</h3>
            <#if This.isTraceEnabled()>
              Traces are enabled : <A href="doc/toggleTraces"> Click here to disable</A><br/>
              <A target="traces"  href="doc/traces?opId=${operation.id}"> Get traces </A>
            <#else>
              Traces are disabled : <A href="doc/toggleTraces"> Click here to enable</A><br/>
              <A target="traces"  href="doc/traces?opId=${operation.id}"> Get light traces </A>
            </#if>

          <#else>
            <h2>Operations list</h2>
            <ul>
            <#list operations as item>
              <li><a href="?id=${item.id}">${item.label}</a></li>
            </#list>
            </ul>
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
      </nav>
      <div class="clearfix" />
     </footer>
  </body>
</html>