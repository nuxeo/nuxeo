OS := $(shell uname)

postgresql:
	printf "$'\npostgresql:\n  deploy: true" >> values.yaml
mongodb:
	printf "$'\nmongodb:\n  deploy: true" >> values.yaml		
redis:
	printf "$'\nredis:\n  deploy: true" >> values.yaml	
helm: 
	jx step helm build
