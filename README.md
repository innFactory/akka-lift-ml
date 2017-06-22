# akka ML Microservice #

Repo for a akka microservice that lift the trained spark ml as a actorsystem with http endpoints.

## Useful commands ##

- sbt -> reStart / reStop             : Start and stop the system easily
- sbt -> ~reStart                     : Start and reload after source changes
- sbt -> reload / update              : Reload build.sbt and update dependencies
- sbt -> dependencyTree               : Print the dependency tree

- curl -i localhost:8080/users        : List all users
- curl -i localhost:8080/users -H "Content-Type: application/json" -d '{"name": "Miel"}'    : Add new user
- curl -i localhost:8080 -X DELETE    : Nicely shutdown the Akka application

[Swagger UI](http://localhost:8080/swagger/)

## Other ##
by [innFactory](https://innfactory.de)
