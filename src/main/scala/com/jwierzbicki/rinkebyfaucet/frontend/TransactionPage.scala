/*
 * Made by Jakub Wierzbicki @jwierzb
 */

/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.frontend

import com.jwierzbicki.rinkebyfaucet.model.EtherModel.TransactionHashCode
import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import com.jwierzbicki.rinkebyfaucet.actor.EthernetConnector

import scala.concurrent.Promise






/**
  * Actor composing transaction page
  */
class TransactionPageActor(context: ActorContext[TransactionPage.TransactionPageData], requestContext: RequestContext, promise: Promise[RouteResult]) extends AbstractBehavior[TransactionPage.TransactionPageData]
{

  private var transactionStatus: String = "N/A"

  val bodyShape: String = "<html><body><h>{transactionStatus}</h></body></html>"


  //Actors behaviour
  override def onMessage(msg: TransactionPage.TransactionPageData): Behavior[TransactionPage.TransactionPageData] = {
    import TransactionPage._

    msg match
    {
      case Success(hash) =>
        transactionStatus = s"Transaction successful. Transaction hash: ${hash.value}"
        compose()
        Behaviors.same
      case InvalidPublicKey(value) =>
        transactionStatus = s"Invalid public key: ${value}"
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
      case _ =>
        transactionStatus = "Internal server error"
        compose()
        Behaviors.same
    }
  }

  //Compose transaction page
  protected def compose(): Unit = {

    val pattern = "\\{transactionStatus\\}".r
    var body = pattern.replaceAllIn(bodyShape, transactionStatus)

    import context.executionContext
    val f = requestContext.complete(OK, HttpEntity(ContentTypes.`text/html(UTF-8)`,body))
    f.onComplete(promise.complete(_))
  }

}




object TransactionPage{

  //Transaction Page Actor messages
  sealed trait TransactionPageData
  final case class ShutdownActor() extends TransactionPageData
  sealed trait TransactionStatus extends TransactionPageData
  final case class Success(hash: TransactionHashCode) extends TransactionStatus

  sealed trait Failed extends TransactionStatus
  final case class InvalidPublicKey(value: String) extends Failed
  final case class ServerError(message: String) extends Failed
  final case class JsonRPCError(message: String) extends Failed

  //Factor method
  def TransactionPage(rc: RequestContext, p: Promise[RouteResult]): Behavior[TransactionPageData] = Behaviors.setup(ctx => new TransactionPageActor(ctx, rc, p))

}