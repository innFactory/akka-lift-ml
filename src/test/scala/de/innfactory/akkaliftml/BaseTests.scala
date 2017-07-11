package de.innfactory.akkaliftml

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._

abstract class MLSpec extends FlatSpec with Matchers with OptionValues

abstract class AkkaSpec extends TestKit(ActorSystem("akkaSpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
