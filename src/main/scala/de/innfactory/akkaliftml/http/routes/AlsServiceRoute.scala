package de.innfactory.akkaliftml.http.routes


import javax.ws.rs.Path

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import de.innfactory.akkaliftml.http.SecurityDirectives
import de.innfactory.akkaliftml.services.als.AlsService._
import de.innfactory.akkaliftml.models.AlsModel
import io.circe.generic.auto._
import io.swagger.annotations._
import akka.pattern.ask
import akka.util.Timeout
import de.innfactory.akkaliftml.utils.Configuration

import scala.concurrent.ExecutionContext

@Path("als")
@Api(value = "/als", produces = "application/json")
class AlsServiceRoute(alsService: ActorRef)(implicit executionContext: ExecutionContext) extends FailFastCirceSupport with Configuration {

  implicit val timeout = Timeout(alsTimeout)

  val route = trainWithModel ~ trainingStatus ~ recommendForUser ~ reload

  @ApiOperation(value = "Get the training status of als service", notes = "", nickname = "trainingStatusALS", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Current Training Status of ALS Trainer", response = classOf[TrainingResponse]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def trainingStatus =
    path("als") {
      get {
        complete {
          (alsService ? GetCurrentStatus).mapTo[TrainingResponse]
        }
      }
    }

  @ApiOperation(value = "Reload latest model from database", notes = "", nickname = "reloadALS", httpMethod = "PUT")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Started reloading the latest als model", response = classOf[TrainingResponse]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def reload =
    path("als") {
    put {
      complete {
        (alsService ? Init()).mapTo[TrainingResponse]
      }
    }
  }

  @ApiOperation(value = "Train a ALSModel with a Model Information", notes = "", nickname = "trainWithModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "TrainingModel Object with training information", required = true, dataType = "de.innfactory.akkaliftml.models.AlsModel", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Training Successfully started", response = classOf[TrainingResponse]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def trainWithModel =
    path("als") {
      post {
        entity(as[AlsModel]) { item =>
          complete {
            (alsService ? TrainWithModel(item)).mapTo[TrainingResponse]
          }
        }
      }
    }

  @Path("/{userId}")
  @ApiOperation(value = "Get Top Recommendations for a user.", notes = "", nickname = "recommendForUser", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "count", value = "recommendation count", required = false, dataType = "integer", paramType = "query"),
    new ApiImplicitParam(name = "userId", value = "user id for recommendations", required = true, dataType = "integer", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Training Successfully started", response = classOf[Recommendations]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def recommendForUser =
    path("als" / Segment) { userId =>
      parameters('count.as[Int] ? 20) { count =>
        get {
          complete {
            (alsService ? RecommendProductsForUsers(userId.toInt, count)).mapTo[Recommendations]
          }
        }
      }
    }


}
