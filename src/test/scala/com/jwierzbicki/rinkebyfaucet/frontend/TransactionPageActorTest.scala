/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.frontend

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.server.Directives.get
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.jwierzbicki.rinkebyfaucet.frontend.MainPage.{GetMainPage, MainPageData}
import com.jwierzbicki.rinkebyfaucet.frontend.TransactionPage.{InvalidPublicKey, ServerError, Success, TransactionPageData}
import com.jwierzbicki.rinkebyfaucet.model.EtherModel.TransactionHashCode
import org.scalatest.{FunSuite, Matchers, WordSpec}

import scala.concurrent.Promise

class TransactionPageActorTest extends WordSpec with Matchers with ScalatestRouteTest {



  // important to provide actor system (untyped(implicit) wont work!)
  var testKit = ActorTestKit()

  val message1  = Success(TransactionHashCode("valid hashcode"))
  val message2  = InvalidPublicKey("invalid hashcode")
  val message3  = ServerError("server error")

  def route(mess: TransactionPageData): Route = {
    get {ctx => {
      val p = Promise[RouteResult]
      val actorRef = testKit.spawn(TransactionPage.TransactionPage(ctx, p))
      actorRef ! mess
      p.future
    }}
  }

  "Transaction page" must {
    "show successful transaction" in {
      Get() ~> route(message1)~> check{
        responseAs[String] shouldEqual "<html><body><h>Transaction successful. Transaction hash: valid hashcode</h></body></html>"
      }

    }
    "show invalid public kay page" in {
      Get() ~> route(message2)~> check{
        responseAs[String] shouldEqual "<html><body><h>Invalid public key: invalid hashcode</h></body></html>"
      }
    }
    "show internal server error" in {
      Get() ~> route(message3)~> check{
        responseAs[String] shouldEqual "<html><body><h>Internal server error</h></body></html>"
      }
    }
    "show internal server error" in {
      Get() ~> route(message3)~> check{
        responseAs[String] shouldEqual "<html><body><h>Internal server error</h></body></html>"
      }
    }
  }

}
