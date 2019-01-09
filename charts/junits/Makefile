OS := $(shell uname)

postgresql:
	echo "$'\npostgresql:\n  deploy: true" >> values.yaml
mongodb:
	echo "$'\nmongodb:\n  deploy: true" >> values.yaml		
helm: 
	jx step helm build
