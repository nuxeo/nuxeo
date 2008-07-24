
package org.nuxeo.ecm.flex.sample.dto
{
	import mx.collections.ArrayCollection;

	public class BaseDummyBean
	{
		protected var _myField:String;

		public function doSomething():void
		{
			_myField = "value changed by local method";
		}
	}	
}
