package de.innfactory.akkaliftml.train

import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DoubleType, IntegerType}

import scala.concurrent.{Future, blocking}
import scala.reflect.io.Path


object ALSTrainer extends App {
  val parser = new scopt.OptionParser[TrainingModel]("ALSTrainer") {
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
  parser.parse(args, TrainingModel()) map { trainingModel =>
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

  def sparkJob(model: TrainingModel): (Double, MatrixFactorizationModel) = {
    val path: Path = Path ("metastore_db/dbex.lck")
    if(path.exists){
      path.delete()
    }
    println(model.sparkMaster)
    val warehouseLocation = "file:${system:user.dir}/spark-warehouse"
    val spark = SparkSession
      .builder()
      .appName("ALSTRAINER1")
      //.master("spark://localhost:7077")
      .master(model.sparkMaster)
      .config("spark.sql.warehouse.dir", warehouseLocation)
      .config("spark.jars", "/Users/Tobias/Developer/akka-ml/target/scala-2.11/akkaliftml_2.11-0.1-SNAPSHOT.jar")
      .config("spark.executor.memory", "4g")
      .enableHiveSupport()
      .getOrCreate()

    import spark.implicits._
    println("<START TRAINING>")
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

    val ratings = spark.read
      .option("header", "true")
      .option("mode", "DROPMALFORMED")
      .format("com.databricks.spark.csv")
      .csv(model.ratings)
      .withColumn("user", 'user.cast(IntegerType))
      .withColumn("product", 'product.cast(IntegerType))
      .withColumn("rating", 'rating.cast(DoubleType))
      .as[Rating].rdd


    val splits = ratings.randomSplit(Array(model.training, model.validation, model.test), 0L)

    val training = splits(0).cache
    val validation = splits(1).cache
    val test = splits(2).cache


    val numTraining = training.count()
    val numValidation = validation.count()
    val numTest = test.count()

    val ranks = model.ranks.toList
    val lambdas = model.lambdas.toList
    val numIters = model.iterations.toList
    var bestModel: Option[MatrixFactorizationModel] = None
    var bestValidationRmse = Double.MaxValue
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1
    for (
      rank <- ranks;
      lambda <- lambdas;
      numIter <- numIters) {
      val model = ALS.train(training, rank, numIter, lambda)
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
    }

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

    println("<END TRAINING>")
    val t1 = System.nanoTime()
    println(s"Elapsed time: ${(t1 - t0) / 1000000} ms")
    spark.stop()
    (bestValidationRmse, bestModel.get)
  }
}