<@extends src="b.ftl">

<@block name="a2">
 a2: Derived Block in c. Document: ${this.title}

</@block>

<@block name="b.nested">
 b.nested: Derived Block in c
</@block>

</@extends>
