package de.innfactory.akkaliftml.als

import akka.actor.ActorLogging
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DoubleType, IntegerType}

import scala.concurrent.{Future, blocking}
import scala.reflect.io.Path


object AlsTrainer extends App {
  val parser = new scopt.OptionParser[AlsModel]("ALSTrainer") {
    head("ALSTrainer", "1.0")
    opt[String]('r', "ratings") required() action { (x, c) =>
      c.copy(ratings = x)
    } text ("ratings is the location of the ratingsfile e.g s3n or hdfs")
    opt[Boolean]('h', "header") action { (x, c) =>
      c.copy(header = x)
    } text ("Header in ratingsfile")
    opt[Double]('t', "training") action { (x, c) =>
      c.copy(training = x)
    } text ("amout of training data from 0.0 to 1.0")
    opt[Double]('v', "validation") action { (x, c) =>
      c.copy(validation = x)
    } text ("amout of validation data from 0.0 to 1.0")
    opt[Double]('i', "test") action { (x, c) =>
      c.copy(test = x)
    } text ("amout of test data from 0.0 to 1.0")
    opt[String]('r', "ranks") action { (x, c) =>
      c.copy(ranks = x.split(",").map(s => s.toInt))
    } text ("ranks for training comma seperated int e.g 1,2,3")
    opt[String]('l', "lambdas") action { (x, c) =>
      c.copy(lambdas = x.split(",").map(s => s.toDouble))
    } text ("lambdas for training comma seperated double e.g 0.1,0.2 ")
    opt[String]('i', "iterations") action { (x, c) =>
      c.copy(iterations = x.split(",").map(s => s.toInt))
    } text ("iterations for training comma seperated int e.g 10,20")
    opt[String]('m', "model") action { (x, c) =>
      c.copy(model = x)
    } text ("model class - MatrixFactorizationModel")
    opt[String]('s', "sparkmaster") action { (x, c) =>
      c.copy(sparkMaster = x)
    } text ("spark master - default local[*]")

  }
  // parser.parse returns Option[C]
  parser.parse(args, AlsModel()) map { trainingModel =>
    print(trainingModel)
    sparkJob(trainingModel)
  } getOrElse {
    // arguments are bad, usage message will have been displayed

  }

  def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating], n: Long): Double = {
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
      .join(data.map(x => ((x.user, x.product), x.rating))).values
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }

  def sparkJob(trainingModel: AlsModel): (Double, String) = {
    val path: Path = Path ("metastore_db/dbex.lck")
    if(path.exists){
      path.delete()
    }
    println(trainingModel.sparkMaster)
    val warehouseLocation = "./spark-warehouse"
    println(s"warehouse ${warehouseLocation}")
    val spark = SparkSession
      .builder()
      .appName("ALSTRAINER1")
      //.master("spark://localhost:7077")
      .master(trainingModel.sparkMaster)
      .config("spark.sql.warehouse.dir", warehouseLocation)
      .config("spark.jars", "/Users/Tobias/Developer/akka-ml/target/scala-2.11/akkaliftml_2.11-0.1-SNAPSHOT.jar")
      .config("spark.executor.memory", "4g")
      .enableHiveSupport()
      .getOrCreate()

    import spark.implicits._
    println("Start Training")
    val t0 = System.nanoTime()
    //spark.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", "AKIAJKXDHJ6VWOCIELMA")
    //spark.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey ", "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")
    val myAccessKey = "AKIAJKXDHJ6VWOCIELMA"
    val mySecretKey = "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"
    val hadoopConf = spark.sparkContext.hadoopConfiguration
    hadoopConf.set("fs.s3.impl", "org.apache.hadoop.fs.s3native.NativeS3FileSystem")
    hadoopConf.set("fs.s3.awsAccessKeyId", myAccessKey)
    hadoopConf.set("fs.s3.awsSecretAccessKey", mySecretKey)

    //ones above this may be deprecated?
    hadoopConf.set("fs.s3n.awsAccessKeyId", myAccessKey)
    hadoopConf.set("fs.s3n.awsSecretAccessKey", mySecretKey)
    println("Hadoop Config was set.")

    val ratings = spark.read
      .option("header", "true")
      .option("mode", "DROPMALFORMED")
      .format("com.databricks.spark.csv")
      .csv(trainingModel.ratings)
      .withColumn("user", 'user.cast(IntegerType))
      .withColumn("product", 'product.cast(IntegerType))
      .withColumn("rating", 'rating.cast(DoubleType))
      .as[Rating].rdd
    println("Ratings loaded.")


    val splits = ratings.randomSplit(Array(trainingModel.training, trainingModel.validation, trainingModel.test), 0L)

    val training = splits(0).cache
    val validation = splits(1).cache
    val test = splits(2).cache
    println("Data splitted.")

    val numTraining = training.count()
    val numValidation = validation.count()
    val numTest = test.count()

    val ranks = trainingModel.ranks.toList
    val lambdas = trainingModel.lambdas.toList
    val numIters = trainingModel.iterations.toList
    val alphas = trainingModel.alphas.toList
    var bestModel: Option[MatrixFactorizationModel] = None
    var bestValidationRmse = Double.MaxValue
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1
    println("will begin trainings...")
    for (
      rank <- ranks;
      lambda <- lambdas;
      numIter <- numIters;
      alpha <- alphas) {
      println("now training with rank = " + rank + ", lambda = " + lambda + ", and numIter = " + numIter + ".")

      val model = trainingModel.trainImplicit match {
        case true => ALS.trainImplicit(training, rank, numIter, lambda, alpha)
        case false => ALS.train(training, rank, numIter, lambda)
      }

      val validationRmse = computeRmse(model, validation, numValidation)
      println("RMSE (validation) = " + validationRmse + " for the model trained with rank = "
        + rank + ", lambda = " + lambda + ", and numIter = " + numIter + ".")
      if (validationRmse < bestValidationRmse) {
        bestModel = Some(model)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
      println("training iteration done!")
    }

    println("evalute the best model on the test set")
    // evaluate the best model on the test set
    val testRmse = computeRmse(bestModel.get, test, numTest)

    println("The best model was trained with rank = " + bestRank + " and lambda = " + bestLambda + ", and numIter = " + bestNumIter + ", and its RMSE on the test set is " + testRmse + ".")

    // create a naive baseline and compare it with the best model
    /* Buffer Overflow, when Spark Cluster has to less memory. Not interesting for me
    val meanRating = training.union(validation).map(_.rating).mean
    val baselineRmse =
      math.sqrt(test.map(x => (meanRating - x.rating) * (meanRating - x.rating)).mean)
    val improvement = (baselineRmse - testRmse) / baselineRmse * 100
    println("The best model improves the baseline by " + "%1.2f".format(improvement) + "%.")
    */


    //val mlmodel = bestModel.get
    //mlmodel.predict(17908, 20)
    //model.save(sc, "/storage/data/modelals2")

    println("training finished")
    val t1 = System.nanoTime()
    println(s"Elapsed time: ${(t1 - t0) / 1000000} ms")
    val savePath = "s3n://innfustest/" + "ALS" + System.nanoTime()
    bestModel.get.save(spark.sparkContext, savePath)
    println("model saved")
    val t2 = System.nanoTime()
    println(s"Elapsed time: ${(t2 - t1) / 1000000} ms")
    spark.stop()
    println("spark stopped")
    (bestValidationRmse, savePath)
  }
}