package de.innfactory.akkaliftml


import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import de.innfactory.akkaliftml.http.HttpService
import de.innfactory.akkaliftml.services.AuthService
import de.innfactory.akkaliftml.services.als.AlsService
import de.innfactory.akkaliftml.utils.AutoValidate
import de.innfactory.akkaliftml.utils.InMemoryPostgresStorage._
import org.scalatest.{Matchers, WordSpec, WordSpecLike}


trait BaseServiceTest extends WordSpec with Matchers with ScalatestRouteTest with FailFastCirceSupport {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  dbProcess.getProcessId

  val alsService = TestActorRef(AlsService.props)
  val authService = new AuthService(new AutoValidate)
  val httpService = new HttpService(authService, alsService)

}
