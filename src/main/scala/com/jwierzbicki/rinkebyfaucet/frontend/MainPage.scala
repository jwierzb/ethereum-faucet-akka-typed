/*
 * Made by Jakub Wierzbicki @jwierzb
 */

/*
 * Made by Jakub Wierzbicki @jwierzb
 */

/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.frontend

import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{RequestContext, RouteResult}

import scala.concurrent.Promise






/**
  * Actor composing page with faucet form
  */
class MainPageActor(context: ActorContext[MainPage.MainPageData], requestContext: RequestContext, promise: Promise[RouteResult]) extends AbstractBehavior[MainPage.MainPageData]
{

  var faucetFormPage: String = """<form method="post" action="" name="form" id="id2"><input type="text" name="public_key"><input type="submit" value="Get Ether"></form>"""

  override def onMessage(msg: MainPage.MainPageData): Behavior[MainPage.MainPageData] = {
    import MainPage._

    msg match
    {
      case GetMainPage() =>
        compose()
        Behaviors.same
        /*
        Killing actor
         */
      case ShutdownActor() =>
        Behaviors.stopped {
          Behaviors.receiveSignal {
            case (ctx, PostStop) =>
              Behaviors.same
          }
        }
    }
  }

  protected def compose(): Unit = {
    import context.executionContext
    val f = requestContext.complete(OK, HttpEntity(ContentTypes.`text/html(UTF-8)`, faucetFormPage))
    f.onComplete(promise.complete(_))
  }

}

object MainPage{

  //Main page actor messages
  sealed trait MainPageData
  final case class GetMainPage() extends MainPageData
  final case class ShutdownActor() extends MainPageData

  //Factor method
  def MainPage(rc: RequestContext, p: Promise[RouteResult]): Behavior[MainPageData] = Behaviors.setup(ctx => new MainPageActor(ctx, rc, p))
}


