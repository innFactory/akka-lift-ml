package de.innfactory.akkaliftml

import akka.actor.Props
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import de.innfactory.akkaliftml.als.AlsActor.TrainingResponse
import de.innfactory.akkaliftml.als._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class AlsServiceTest extends FlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll with DefaultJsonFormats {

  val alsActor = system.actorOf(Props[AlsActor])
  val alsService = new AlsService(alsActor)
  implicit val trainingRepsonse = jsonFormat2(TrainingResponse)
  implicit val trainingModel = jsonFormat10(AlsModel)

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "ALSService" should "not train" in {
    Get(s"/als") ~> alsService.route ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
    }
  }

  it should "start training" in {
    Post(s"/als", AlsModel("retail-raiting.csv")) ~> alsService.route ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[TrainingResponse].train shouldBe true
    }
  }

}
