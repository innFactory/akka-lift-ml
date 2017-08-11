package de.innfactory.akkaliftml

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import de.innfactory.akkaliftml.models.AlsModel
import io.circe.generic.auto._

class AlsServiceTest extends BaseServiceTest {

  trait Context {
    val route = httpService.alsRouter.route
  }

  "ML ALS Service" should {
    "should not train" in new Context {
      Get(s"/als") ~> route ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
      }
    }

    "start training" in new Context {
      Post(s"/als", AlsModel("retail-raiting.csv")) ~> route ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
      }
    }
  }
}

