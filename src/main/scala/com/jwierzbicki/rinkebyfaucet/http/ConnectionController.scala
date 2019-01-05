/*
 * Made by Jakub Wierzbicki @jwierzb
 */


package com.jwierzbicki.rinkebyfaucet.http

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.actor.typed.scaladsl.ActorContext
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes._
import com.jwierzbicki.rinkebyfaucet.actor.EthernetConnector
import com.jwierzbicki.rinkebyfaucet.frontend.{MainPage, TransactionPage}
import com.jwierzbicki.rinkebyfaucet.model._
import com.jwierzbicki.rinkebyfaucet.model.EtherModel.PublicKey

import scala.concurrent.Promise


/*
Web application controller trait
 */
trait ConnectionController {

  implicit val actorSystemUntyped: ActorSystem
  implicit val actorContext: ActorContext[Nothing]

  //definition of application endpoints
  val mainRoute: Route =
    path("faucet") {
      get {
        faucetGet(actorContext)
      } ~
      post {
        formField("public_key") {
          pk => faucetPost(TransferRequest(PublicKey(pk)), actorContext)
        }
      }
    } ~ complete(BadRequest, HttpEntity(ContentTypes.`text/html(UTF-8)`, "<html><body><h>404</h></body></html>"))


  // /faucet post endpoint request handler
  def faucetPost(request: TransferRequest, ac: ActorContext[Nothing]): Route = ctx => {
    val p = Promise[RouteResult]
    val ethActor = ac.spawn(EthernetConnector.withJsonRPC() , s"ethernetConnectorActor-${UUID.randomUUID().toString}")
    val frontActor = ac.spawn(TransactionPage.TransactionPage(ctx, p) , s"frontEndActor-${UUID.randomUUID().toString}")

    ethActor ! EthernetConnector.TransferCoins(request.key, frontActor)
    ethActor ! EthernetConnector.ShutdownActor()

    p.future
  }
  // /faucet get endpoint request handler
  def faucetGet(ac: ActorContext[Nothing]): Route = ctx => {
    val p = Promise[RouteResult]
    val mainPageActor = ac.spawn(MainPage.MainPage(ctx, p), s"frontEndActor-${UUID.randomUUID().toString}")
    mainPageActor ! MainPage.GetMainPage()
    mainPageActor ! MainPage.ShutdownActor()
    p.future
  }
}







