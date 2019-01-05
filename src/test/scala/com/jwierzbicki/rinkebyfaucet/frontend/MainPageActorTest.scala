/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.frontend

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.server.{Directives, Route, RouteResult}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.jwierzbicki.rinkebyfaucet.frontend.MainPage.{GetMainPage, MainPageData}
import Directives._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Promise

class MainPageActorTest extends WordSpec with Matchers with ScalatestRouteTest{


  // important to provide actor system (untyped(implicit) wont work!)
  var testKit = ActorTestKit()

  val message1  = GetMainPage()

  def route(mess: MainPageData): Route = {
    get {ctx => {
      val p = Promise[RouteResult]
      val actorRef = testKit.spawn(MainPage.MainPage(ctx, p))
      actorRef ! mess
      p.future
    }}
  }

  "MainPageActor" must {
    "compose valid main page" in {
      Get() ~> route(message1)~> check{
        responseAs[String] shouldEqual """<form method="post" action="" name="form" id="id2"><input type="text" name="public_key"><input type="submit" value="Get Ether"></form>"""
      }

    }
  }


}
