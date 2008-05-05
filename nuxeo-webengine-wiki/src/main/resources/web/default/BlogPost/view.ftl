<@extends src="/default/Blog/base.ftl">
<@block name="content">
  <h1>${this.dublincore.title}</h1>
  ${this.blogPost.content}
</@block>
</@extends>
