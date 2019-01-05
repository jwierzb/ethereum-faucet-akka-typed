/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.http

import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.io.StdIn


/**
  * Class providing the http server existance by binding http requests with ConnectionController.mainRoute
  */
class Server(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing] with ConnectionController {

  //Typed actor system adapter
  import akka.actor.typed.scaladsl.adapter._

  override implicit val actorContext = context
  override implicit val actorSystemUntyped = context.system.toUntyped
  implicit val materializer = ActorMaterializer()(actorSystemUntyped)
  implicit val executionContext = context.executionContext

  //Start the server
  start()

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    // No need to handle any messages
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop ⇒
      context.log.info("Application stopped")
      Behaviors.same
  }


  //Method configuring and binding the server with ConnectionController.mainRoute
  def start(): Unit = {
    val config = ConfigFactory.load()

    val addres = config.getString("http.routing.addres")
    val port = config.getInt("http.routing.port")


    //Bind every connections with ConnectionController.mainRoute
    val bindingFuture: Future[ServerBinding] = Http(actorSystemUntyped).bindAndHandle(mainRoute, addres, port)

    bindingFuture.onComplete({
      case scala.util.Success(value)  =>{
        println(s"Server started on addres ${addres} on port ${port}")
        context.log.info(s"Server started on addres ${addres} on port ${port}")
      }
      case scala.util.Failure(exception) => {
        context.log.error(exception, s"Failed to bind to ${addres}:${port}!")
        println("Server failed to bind")
      }
    })

    println("Press enter to terminate")
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => actorSystemUntyped.terminate())
  }

}


object Server{

  //Factor method
  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing]
    {
      ctx => {
        ctx.log.info("Application started")
        new Server(ctx)
      }
    }
}
