package de.innfactory.akkaliftml.als

import akka.actor.{Actor, ActorLogging}
import de.innfactory.akkaliftml.ActorSettings
import de.innfactory.akkaliftml.als.AlsActor.{LoadTrainedModel, TrainWithModel, UpdateStatus}
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DoubleType, IntegerType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}
import scala.reflect.io.Path

object AlsTrainingActor {

}

class AvgCollector(val tot: Double, val cnt: Int = 1) extends Serializable {
  def ++(v: Double) = new AvgCollector(tot + v, cnt + 1)
  def combine(that: AvgCollector) = new AvgCollector(tot + that.tot, cnt + that.cnt)
  def avg = if (cnt > 0) tot / cnt else 0.0
}

class AlsTrainingActor extends Actor with ActorLogging with ActorSettings {

  override def receive: Receive = {
    case TrainWithModel(model) => {
      val origSender = sender
      Future {
        blocking {
          origSender ! UpdateStatus(true)
          sparkJob(model)
        }
      }.map(model => {
        log.info(s"Got a new model with RMSE ${model._1}")
        origSender ! LoadTrainedModel(model._1, model._2)
        origSender ! UpdateStatus(false)
      }).recoverWith {
        case e: Exception => {
          origSender ! UpdateStatus(false)
          log.error("Training failed with Exception: {}", e)
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
    log.info(s"Training started on ${trainingModel.sparkMaster.getOrElse("local")}")
    println(">>"+settings.spark.jar)
    val spark = SparkSession
      .builder()
      .appName(s"${settings.spark.appName}Trainer")
      .master(trainingModel.sparkMaster.getOrElse("local[*]"))
      .config("spark.jars", s"${settings.spark.jar}")
      .config("spark.executor.memory", s"${settings.spark.executorMemory}")
      .getOrCreate()

    import spark.implicits._

    val t0 = System.nanoTime()

    log.info("Set values to hadoop config")
    val hadoopConf = spark.sparkContext.hadoopConfiguration
    hadoopConf.set("fs.s3.impl", "org.apache.hadoop.fs.s3native.NativeS3FileSystem")
    hadoopConf.set("fs.s3.awsAccessKeyId", settings.aws.accessKeyId)
    hadoopConf.set("fs.s3.awsSecretAccessKey", settings.aws.secretAccessKey)
    hadoopConf.set("fs.s3n.awsAccessKeyId", settings.aws.accessKeyId)
    hadoopConf.set("fs.s3n.awsSecretAccessKey", settings.aws.secretAccessKey)

    val ratings = spark.read
      .option("header", true)
      .option("mode", "DROPMALFORMED")
      .format(settings.fs.ratingsFormat)
      .csv(trainingModel.ratings)
      .withColumn("user", 'user.cast(IntegerType))
      .withColumn("product", 'product.cast(IntegerType))
      .withColumn("rating", 'rating.cast(DoubleType))
      .as[Rating].rdd

    log.info("Ratings loaded.")


    val splits = ratings.randomSplit(Array(trainingModel.training.getOrElse(0.8), trainingModel.validation.getOrElse(0.1), trainingModel.test.getOrElse(0.1)), 0L)

    val trainingCold = splits(0).cache
    log.info("calculating best default values for cold starting users.")
    val best = trainingCold
      .map(r => (r.product, r.rating))
      .aggregateByKey( new AvgCollector(0.0,0) )(_ ++ _, _ combine _ )
      .map{ case (k,v) => (k, v.avg) }
      .sortBy(_._2, false).take(5)
      .map(m => Rating(0,m._1,m._2))
    log.info("add value for cold starting users to the training data")
    val training = trainingCold ++ spark.sparkContext.parallelize(best.toList)


    val validation = splits(1).cache
    val test = splits(2).cache
    log.info("Data was splitted")

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
    log.info("training with given parameters will start.")
    for (
      rank <- ranks;
      lambda <- lambdas;
      numIter <- numIters;
      alpha <- alphas) {
      log.info(s"now training with rank = $rank, lambda = $lambda, and numIter = $numIter")

      val model = trainingModel.trainImplicit.getOrElse(false) match {
        case true => ALS.trainImplicit(training, rank, numIter, lambda, alpha)
        case false => ALS.train(training, rank, numIter, lambda)
      }

      val validationRmse = computeRmse(model, validation, numValidation)
      log.info(s"RMSE (validation) = $validationRmse for the model trained with rank = $rank, lambda = $lambda, and numIter = $numIter ")
      if (validationRmse < bestValidationRmse) {
        bestModel = Some(model)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
      log.info("training iteration done!")
    }

    log.info("evalute the best model on the test set")
    // evaluate the best model on the test set
    val testRmse = computeRmse(bestModel.get, test, numTest)

    log.info(s"The best model was trained with rank = $bestRank and lambda = $bestLambda, and numIter = $bestNumIter, and its RMSE on the test set is $testRmse.")


    log.info("training finished")
    val t1 = System.nanoTime()
    log.info(s"Elapsed time for training: ${(t1 - t0) / 1000000} ms")
    val savePath = s"${settings.aws.location}ALS${System.nanoTime()}"
    bestModel.get.save(spark.sparkContext, savePath)
    log.info("model saved")
    val t2 = System.nanoTime()
    log.info(s"Elapsed time for saving: ${(t2 - t1) / 1000000} ms")
    spark.stop()
    log.info("Spark stopped")
    (bestValidationRmse, savePath)
  }

}
