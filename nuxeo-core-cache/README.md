nuxeo-core-cache
=========================

## What is this project ?

Nuxeo Core Cache is a bundle of the Nuxeo Platforms that allows to set-up cache management system ahead of cache API such as Google Guava or Redis

The aim is to provide a powerful centralized cache manager system to be integrated in a lot of code (Directory, UserManager ...) 

## Why would you use this ?


Typical use case ensure that wherever the layer you need caching, you may get a pre-declared cache or defined your new cache by selecting its class implementation ahead of one of the embedded cache framework :
 - Google Guava (standard implementation of the UserManagerImpl that does not support distributed cache)
 - Redis (Use Redis for distributed cache purpose)

You may provide new CacheManager implementation if needed by extending the abstract class AbstractCacheManager and then add a new contrib with the given implClass name pointing to your implementation.

(useful sample configuration can be found in the src/test/resources folder)
The configuration require the following steps :
    

## Prerequisite 
Prerequisite to use the cache manager:
	- define mandatory attributes
	- get your cache manager by calling the CacheManagerService

## History

This code was initially written against a Nuxeo 5.9.6 


