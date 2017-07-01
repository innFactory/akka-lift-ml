package de.innfactory.akkaliftml.als

import akka.actor.{Actor, ActorLogging}
import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global

object AlsActor {

  case class TrainWithModel(name: AlsModel)

  case class TrainingResponse(responseMessage: String, train: Boolean)

  case class Recommendations(recommendations: Array[(Int, Double)])

  case class GetCurrentStatus()

  case class SaveModel(rmse : Double, model : String)

  case class RecommendProductsForUsers(user : Int, count: Int)

  case class InitSpark()

}

class AlsActor extends Actor with ActorLogging {

  import AlsActor._
  var spark : Option[SparkSession] = None
  var status = false
  var modelRmse = Double.MaxValue
  var currentModelPath : String = ""
  var currentModel : Option[MatrixFactorizationModel] = None
  var currentRecommendations : Option[RDD[(Int,Array[Rating])]] = None

  def initSpark() = {
    spark = Some(SparkSession
      .builder()
      .master("local[*]")
      .appName("Spark2.0")
      .getOrCreate())

    val myAccessKey = "AKIAJKXDHJ6VWOCIELMA"
    val mySecretKey = "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"
    val hadoopConf = spark.get.sparkContext.hadoopConfiguration
    hadoopConf.set("fs.s3.impl", "org.apache.hadoop.fs.s3native.NativeS3FileSystem")
    hadoopConf.set("fs.s3.awsAccessKeyId", myAccessKey)
    hadoopConf.set("fs.s3.awsSecretAccessKey", mySecretKey)

    //ones above this may be deprecated?
    hadoopConf.set("fs.s3n.awsAccessKeyId", myAccessKey)
    hadoopConf.set("fs.s3n.awsSecretAccessKey", mySecretKey)
  }

  def receive: Receive = {

    case RecommendProductsForUsers(user, count) => {
      if(currentModel.isDefined && currentRecommendations.isDefined) {
        println("try to recommend " + count + " items to + user " + user)

        val recommendations = currentRecommendations.get.filter(f => f._1 == user).take(count).flatMap(r => r._2).map(r => (r.product, r.rating))
        println("recommendations found")
        sender ! Recommendations(recommendations)
        recommendations.foreach(r => println(r._1 + " :: " + r._2))
      }else{
        sender ! Recommendations(Array())
      }
    }
    case SaveModel(rmse, model) => {
      if(!spark.isDefined) {
        println("init local spark")
        initSpark()
      }
      println("check rmse")
      if(rmse < modelRmse) {
        println("save new model")
        currentModelPath = model
        println("load new from s3")
        currentModel = Some( MatrixFactorizationModel.load(spark.get.sparkContext, currentModelPath))
        println("new model initialized")
        currentModel.get.userFeatures.cache()
        currentModel.get.productFeatures.cache()
        println("cache... ")
        currentRecommendations = Some(currentModel.get.recommendProductsForUsers(100).cache())
        print(".")
        currentRecommendations.get.collect()
        println("cached")
        modelRmse = rmse
      }
    }
    case TrainWithModel(model) => {
      if (status) {
        sender ! TrainingResponse(s"Training is running, please wait!", status)
      } else {
        sender ! TrainingResponse(s"Training started with ${model.toString}", status)
        status = true
        Future {
          blocking {
            AlsTrainer.sparkJob(model)
          }
        }.map(model => {
          println("got a new model with RMSE - ")
          println(model._1)
          println("Save new model")
          self ! SaveModel(model._1, model._2)
          status = false
        }).recoverWith {
          case e: Exception => {
            println("task failed")
            status = false
            Future.failed(e)
          }
        }
      }
    }
    case GetCurrentStatus => sender ! TrainingResponse(s"Training is running [${status}] Current best RMSE (${modelRmse})", status)
  }


}