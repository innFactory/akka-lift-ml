package de.innfactory.akkaliftml.als

import akka.actor.{Actor, ActorLogging, Props}
import de.innfactory.akkaliftml.ActorSettings
import de.innfactory.akkaliftml.util.MLPreferences
import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.ExecutionContext.Implicits.global

object AlsActor {

  case class TrainWithModel(name: AlsModel)

  case class TrainingResponse(responseMessage: String, train: Boolean)

  case class Recommendations(recommendations: Array[(Int, Double)])

  case class GetCurrentStatus()

  case class LoadTrainedModel(rmse: Double, model: String)

  case class RecommendProductsForUsers(user: Int, count: Int)

  case class InitSpark()

  case class UpdateStatus(bool: Boolean)

  case class Init()

}

class AlsActor extends Actor with ActorLogging with ActorSettings {

  import AlsActor._

  var spark: Option[SparkSession] = None
  var status = false
  var modelRmse = Double.MaxValue
  var currentModelPath: String = ""
  var currentModel: Option[MatrixFactorizationModel] = None
  var currentRecommendations: Option[RDD[(Int, Array[Rating])]] = None

  val trainer = context.actorOf(Props[AlsTrainingActor])

  def receive: Receive = {
    case Init() => {
      val model = MLPreferences.getCurrentALSModel()
      log.info(s"init als trainer with pref >> $model")
      model match {
        case "" | "test" => log.info("no dataset found")
        case alsModel: String => loadDataFromModel(alsModel)
      }
    }
    case RecommendProductsForUsers(user, count) => {
      if (currentModel.isDefined && currentRecommendations.isDefined) {
        log.info(s"try to recommend ${count} items to user: ${user}")
        val recommendations = currentRecommendations.get.filter(f => f._1 == user).flatMap(r => r._2).map(r => (r.product, r.rating))
        if (recommendations.count == 0) {
          log.info("No recommendations found. taking default values")
          val recommendationsDefault = currentRecommendations.get.filter(f => f._1 == 0).take(count).flatMap(r => r._2).map(r => (r.product, r.rating))
          sender ! Recommendations(recommendationsDefault.take(count))
        } else {
          sender ! Recommendations(recommendations.take(count))
        }
      } else {
        sender ! Recommendations(Array())
      }
    }
    case LoadTrainedModel(rmse, model) => {
      loadDataFromModel(model, rmse)
    }

    case UpdateStatus(update) => {
      log.info(s"change training status to $update")
      status = update
    }

    case TrainWithModel(model) => {
      if (status) {
        sender ! TrainingResponse(s"Training is running, please wait!", status)
      } else {
        sender ! TrainingResponse(s"Training started with ${model.toString}", true)
        trainer ! TrainWithModel(model)
      }
    }
    case GetCurrentStatus => sender ! TrainingResponse(if (status) "Training is running" else "Training is not running", status)
  }

  def getNewSpark: SparkSession = {
    val newSpark = SparkSession
      .builder()
      .master("local[*]")
      .appName(settings.spark.appName)
      .getOrCreate()

    val hadoopConf = newSpark.sparkContext.hadoopConfiguration
    hadoopConf.set("fs.s3.impl", "org.apache.hadoop.fs.s3native.NativeS3FileSystem")
    hadoopConf.set("fs.s3.awsAccessKeyId", settings.aws.accessKeyId)
    hadoopConf.set("fs.s3.awsSecretAccessKey", settings.aws.secretAccessKey)
    hadoopConf.set("fs.s3n.awsAccessKeyId", settings.aws.accessKeyId)
    hadoopConf.set("fs.s3n.awsSecretAccessKey", settings.aws.secretAccessKey)
    newSpark
  }

  def loadDataFromModel(model: String, rmse: Double = 0.0): Unit = {
    Future {
      blocking {
        val newSpark = getNewSpark
        if (rmse < modelRmse) {
          log.warning("Learning a model with higher Error")
        }

        val newCurrentModelPath = model
        val newCurrentModel = Some(MatrixFactorizationModel.load(newSpark.sparkContext, newCurrentModelPath))
        newCurrentModel.get.userFeatures.cache()
        newCurrentModel.get.productFeatures.cache()
        val newCurrentRecommendations = Some(newCurrentModel.get.recommendProductsForUsers(100).cache())
        newCurrentRecommendations.get.collect()
        val newModelRmse = rmse

        MLPreferences.setCurrentALSModel(newCurrentModelPath)

        currentModelPath = newCurrentModelPath
        currentModel = newCurrentModel
        currentRecommendations = newCurrentRecommendations
        modelRmse = newModelRmse
        spark = Some(newSpark)
      }
    }
  }

}