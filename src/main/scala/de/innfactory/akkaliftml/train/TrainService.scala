package de.innfactory.akkaliftml.train

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorRef
import akka.util.Timeout
import akka.http.scaladsl.model.Uri.Path.Segment
import akka.http.scaladsl.server.Directives
import io.swagger.annotations._
import javax.ws.rs.Path

import de.innfactory.akkaliftml
import de.innfactory.akkaliftml.DefaultJsonFormats
import de.innfactory.akkaliftml.hello.HelloActor.{Greeting, Hello}
import de.innfactory.akkaliftml.train.TrainActor._
import org.apache.spark.mllib.recommendation.Rating

@Path("/trainer")
@Api(value = "/trainer", produces = "application/json")
class TrainService(trainer: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with DefaultJsonFormats {

  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val timeout = Timeout(100.seconds)

  import spray.json.DefaultJsonProtocol._

  implicit val trainingsRepsonseFormat = jsonFormat2(TrainingResponse)
  implicit val trainingModel = jsonFormat10(TrainingModel)
  implicit val rating = jsonFormat3(Rating)
  implicit val recommendationsModel = jsonFormat1(Recommendations)

  val route = trainWithModel ~ trainingStatus ~ recommendForUser


  @ApiOperation(value = "Get the status of current training", notes = "", nickname = "trainingStatus", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Current Training Status", response = classOf[TrainingResponse]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def trainingStatus =
    path("trainer") {
      get {
        complete {
          (trainer ? GetCurrentStatus).mapTo[TrainingResponse]
        }
      }
    }

  @ApiOperation(value = "Train a Model with a Model", notes = "", nickname = "trainWithModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "TrainingModel Object with training information", required = true, dataType = "de.innfactory.akkaliftml.train.TrainingModel", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Training Successfully started", response = classOf[TrainingResponse]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def trainWithModel =
    path("trainer") {
      post {
        entity(as[TrainingModel]) { item =>
          complete {
            (trainer ? TrainWithModel(item)).mapTo[TrainingResponse]
          }
        }
      }
    }

  @Path("/{userId}")
  @ApiOperation(value = "Train a Model with a Model", notes = "", nickname = "recommendForUser", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "count", value = "recommendation count", required = false, dataType = "integer", paramType = "query"),
    new ApiImplicitParam(name = "userId", value = "user id for recommendations", required = true, dataType = "integer", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Training Successfully started", response = classOf[Recommendations]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def recommendForUser =
    path("trainer" / Segment) { userId =>
      parameters('count.as[Int] ? 20) { count =>
        get {
          complete {
            (trainer ? RecommendProductsForUsers(userId.toInt, count)).mapTo[Recommendations]
          }
        }
      }
    }

}

