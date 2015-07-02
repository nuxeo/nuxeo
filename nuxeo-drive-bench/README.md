# Nuxeo Drive bench

# Launch bench

Default bench against http://localhost:8080/nuxeo

         mvn test


Rampup to 30 concurrent users in 15s, duration of session is 60s, 
thinktime is set to 2s,  this will
give you : 15s ramp up, 45s at 30 users, then ramp down 15:

         mvn test -Dusers=30 -Dduration=60 -Dramp=15 -Dpause=2



# Scenario

TODO
