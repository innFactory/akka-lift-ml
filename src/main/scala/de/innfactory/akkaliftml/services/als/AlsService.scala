package de.innfactory.akkaliftml.services.als

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import de.innfactory.akkaliftml.models.{AlsModel, AlsTraining}
import de.innfactory.akkaliftml.models.db.AlsRepository
import de.innfactory.akkaliftml.utils.{Configuration, Persistence, PersistenceActor}
import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, blocking}

object AlsService {

  case class TrainWithModel(name: AlsModel)

  case class TrainingResponse(responseMessage: String, train: Boolean)

  case class Recommendations(recommendations: Array[(Int, Double)])

  case class GetCurrentStatus()

  case class LoadTrainedModel(rmse: Double, model: String)

  case class RecommendProductsForUsers(user: Int, count: Int)

  case class InitSpark()

  case class UpdateStatus(bool: Boolean)

  case class Init()

  def props(implicit executionContext: ExecutionContext) = Props(new AlsService()(executionContext))

}

class AlsService()(implicit executionContext: ExecutionContext) extends PersistenceActor with ActorLogging with Configuration {

  val alsRepository = new AlsRepository()

  import AlsService._

  var spark: Option[SparkSession] = None
  var status = false
  var modelRmse = Double.MaxValue
  var currentModelPath: String = ""
  var currentModel: Option[MatrixFactorizationModel] = None
  var currentRecommendations: Option[RDD[(Int, Array[Rating])]] = None

  val trainer = context.actorOf(Props[AlsTrainingActor])

  def getCurrentModelFromDatabase(): Future[Seq[AlsTraining]] = {
    executeOperation(alsRepository.findLatestAlsModel())
  }

  def receive: Receive = {
    case Init() => {
      getCurrentModelFromDatabase().map { alsTraining =>
        val latest = alsTraining.head
        log.info("Load model from data")
        loadDataFromModel(latest.path, latest.rmse)
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
      .config("spark.driver.memory", s"${sparkDriverMemory}")
      .appName(sparkAppName)
      .getOrCreate()

    val hadoopConf = newSpark.sparkContext.hadoopConfiguration
    hadoopConf.set("fs.s3.impl", "org.apache.hadoop.fs.s3native.NativeS3FileSystem")
    hadoopConf.set("fs.s3.awsAccessKeyId", awsAccessKeyId)
    hadoopConf.set("fs.s3.awsSecretAccessKey", awsSecretAccessKey)
    hadoopConf.set("fs.s3n.awsAccessKeyId", awsAccessKeyId)
    hadoopConf.set("fs.s3n.awsSecretAccessKey", awsSecretAccessKey)
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


        val alsTraining = AlsTraining(None, newCurrentModelPath, newModelRmse)
        executeOperation(alsRepository.save(alsTraining))

        currentModelPath = newCurrentModelPath
        currentModel = newCurrentModel
        currentRecommendations = newCurrentRecommendations
        modelRmse = newModelRmse
        spark = Some(newSpark)
      }
    }
  }

}
