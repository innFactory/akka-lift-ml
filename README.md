# akka-lift-ml Microservice #

Repository for an akka microservice that lift the trained spark ml algorithms as a actorsystem with http endpoints.

## Description ##
akka-lift-ml helps you with the hard data engineering part, when you have found a good solutions in Data Science.
The service can train your models on a remote spark instance and serve the results with a small local spark service.
You can access it over http e.g. with the integrated swagger ui. 
To build your own system you need sbt and scala.

## Supported ML Algorithms ##
- Collaborative Filtering with ALS (Alternating-Least-Squares)

## QuickStart Guide ##
- If you want to train remote and not on your local machine, first start your Spark Cluster ([Spark Cluster with 1x Master & 3x worker via Docker](https://github.com/innFactory/docker/tree/master/spark-master-worker))
- Checkout the source code from github
- Copy application.example.conf to application.example.conf (resources folder)
- Make related config changes
- If you use AWS be sure that the s3 Bucket is not in EUROPE!! Spark 2.1 can not write/read data then
- create a jar as a spark driver ```sbt package``` - be sure the path in application.conf is set correctly.
- run ```sbt run```
- go to [Swagger UI](http://localhost:8283/swagger/index.html) (http://localhost:8283/swagger/index.html)
- send your request to the service
- after successfull training you get the result via http get 
- run ```sbt docker``` to get a docker container

For more details and instructions read the wiki.

## Contributers ##

Tobias Jonas 

## Other ##
akka-lift-ml is licensed under Apache License, Version 2.0.

Commercial Support [innFactory Cloud & DataEngineering](https://innfactory.de)
