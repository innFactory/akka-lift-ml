package de.innfactory.akkaliftml.als

import akka.actor.{Actor, ActorLogging}
import de.innfactory.akkaliftml.als.AlsActor.{SaveModel, TrainWithModel, UpdateStatus}
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DoubleType, IntegerType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}
import scala.reflect.io.Path

object AlsTrainingActor {

}

class AlsTrainingActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case TrainWithModel(model) => {
      val origSender = sender
      Future {
        blocking {
          origSender ! UpdateStatus(true)
          sparkJob(model)
        }
      }.map(model => {
        println("got a new model with RMSE - ")
        println(model._1)
        println("Save new model")
        origSender ! SaveModel(model._1, model._2)
        origSender ! UpdateStatus(false)
      }).recoverWith {
        case e: Exception => {
          origSender ! UpdateStatus(false)
          println("Training failed...")
          log.error("Exception: {}", e)
          Future.failed(e)
        }
      }
    }
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
      .master(trainingModel.sparkMaster.getOrElse("local[*]"))
      .config("spark.sql.warehouse.dir", warehouseLocation)
      .config("spark.jars", "/Users/Tobias/Developer/akka-ml/target/scala-2.11/akkaliftml_2.11-0.1-SNAPSHOT.jar")
      .config("spark.executor.memory", "4g")
      //.enableHiveSupport()
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
      .option("header", trainingModel.header.getOrElse(true).toString) //TODO FIXME NOT WORKING WITH FALSE
      .option("mode", "DROPMALFORMED")
      .format("com.databricks.spark.csv")
      .csv(trainingModel.ratings)
      .withColumn("user", 'user.cast(IntegerType))
      .withColumn("product", 'product.cast(IntegerType))
      .withColumn("rating", 'rating.cast(DoubleType))
      .as[Rating].rdd
    println("Ratings loaded.")


    val splits = ratings.randomSplit(Array(trainingModel.training.getOrElse(0.8), trainingModel.validation.getOrElse(0.1), trainingModel.test.getOrElse(0.1)), 0L)

    val training = splits(0).cache
    val validation = splits(1).cache
    val test = splits(2).cache
    println("Data splitted.")

    val numTraining = training.count()
    val numValidation = validation.count()
    val numTest = test.count()

    val ranks = trainingModel.ranks.getOrElse(Array({200})).toList
    val lambdas = trainingModel.lambdas.getOrElse(Array({0.1})).toList
    val numIters = trainingModel.iterations.getOrElse(Array({10})).toList
    val alphas = trainingModel.alphas.getOrElse(Array({1.0})).toList
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

      val model = trainingModel.trainImplicit.getOrElse(false) match {
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
