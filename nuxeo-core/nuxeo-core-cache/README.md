nuxeo-core-cache
=========================

## What is this project ?

Nuxeo Core Cache is a bundle of the Nuxeo Platforms that allows to set-up cache management system ahead of cache API such as Google Guava or Redis

The aim is to provide a powerful centralized cache system to be integrated in a lot of code (Directory, UserManager ...) 

## Why would you use this ?


Typical use case ensure that wherever the layer you need caching, you may get a pre-declared cache or defined your new cache by selecting its class implementation on top of one of the embedded cache framework :
 - Google Guava (standard implementation 'org.nuxeo.ecm.core.cache.CacheImpl' that does not support distributed cache)
 - Redis (Use 'org.nuxeo.ecm.core.redis.RedisCacheImpl' for distributed cache purpose)

You may provide new Cache implementation if needed by extending the abstract class AbstractCache and then add a new contrib with the given class name pointing to your implementation in the contrib. Make sure also to provide a unit test class that extends the AbstractTestCache class to match the behavior for caching system in the unit test.

(useful sample configuration can be found in the src/test/resources folder)
The configuration require the following steps :
    
  =>Guava configuration
     
     <extension target="org.nuxeo.ecm.core.cache.CacheServiceImpl"
		point="caches">
		<cache name="my-cache"
			class="org.nuxeo.ecm.core.cache.InMemoryCacheImpl">
			<ttl>20</ttl><!-- in minutes -->
			<option name="maxSize">10</option>
			<option name="concurrencyLevel">10</option>
		</cache>
	</extension>
      
  
  =>Redis configuration
      <br/>Make sure you have a contrib such as below :
      
    <extension target="org.nuxeo.ecm.core.redis.RedisServiceImpl" point="configuration">
      <redis disabled="false">
        <prefix>nuxeo:</prefix>
        <host>localhost</host>
        <port>6379</port>
        <password>secret</password>
        <database>0</database>
        <timeout>2000</timeout>
      </redis>
    </extension>

   The cache below will use the RedisCacheImpl that use the redisService defined as previously :

    <extension target="org.nuxeo.ecm.core.cache.CacheServiceImpl" point="caches">
	    <cache name="my-redis-cache" class="org.nuxeo.ecm.core.redis.RedisCacheImpl">
      	  <ttl>20</ttl><!-- in minutes -->
    	</cache>
  </extension>
  
## Prerequisite 
Prerequisite to use the cache :
<ul>
<li>define mandatory attributes : ttl</li>
<li> define optional attributes :  concurrency level,maxSize</li>
<li>get your cache by calling the cacheService.getCache method</li>
<li>The key of values must be a String</li>
<li>The values must implement the Serializable interface</li>
<li>If you want to use a cache on top of redis, you must have one contrib that define a redis server</li>
<li>If you use redis, make sure your redis server is running</li>
</ul>

## History

This code was initially written against a Nuxeo 5.9.6 


