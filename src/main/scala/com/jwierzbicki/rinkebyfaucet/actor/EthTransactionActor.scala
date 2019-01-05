/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.actor

import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.jwierzbicki.rinkebyfaucet.Json.JsonSupport
import com.jwierzbicki.rinkebyfaucet.actor.EthernetConnector.{ShutdownActor, TransferCoins}
import com.jwierzbicki.rinkebyfaucet.frontend.TransactionPage
import com.jwierzbicki.rinkebyfaucet.frontend.TransactionPage.{InvalidPublicKey, TransactionPageData}
import com.jwierzbicki.rinkebyfaucet.model.EtherModel.{PublicKey, TransactionHashCode}
import com.typesafe.config.ConfigFactory

//Future support imports
import scala.concurrent.Future
import scala.util.{Failure, Success}




/**
  * Actor's trait responsible for connecting with ethernet
  */
trait EthernetConnector extends AbstractBehavior[EthernetConnector.ClientRequest] {


  /**
    * Method sending transaction to wallet with @param publicKey through JsonRPC APi
    * and @return server response
    */
  def sendCoin(publicKey: PublicKey): Future[HttpResponse]

}


object EthernetConnector {

  //Factor method
  def withJsonRPC(): Behavior[ClientRequest] = Behaviors.setup(ctx => new WithJsonRPC(ctx))

  //Actor messages
  sealed trait ClientRequest
  final case class TransferCoins(publicKey: PublicKey, replyTo: ActorRef[TransactionPageData]) extends ClientRequest
  final case class ShutdownActor() extends ClientRequest
}



/**
  * Implementation of EthernetConnector connecting with ethernet node with JsonRPC API
  */
class WithJsonRPC(context: ActorContext[EthernetConnector.ClientRequest]) extends EthernetConnector with JsonSupport {

  import spray.json._
  import com.jwierzbicki.rinkebyfaucet.model._

  import akka.actor.typed.scaladsl.adapter._

  implicit val ac = context.system.toUntyped //untyped actor system
  implicit val ec = context.executionContext
  implicit val ma = ActorMaterializer.create(ac) //actor materializer (bytestrings)


  private lazy val config = ConfigFactory.load()


  override def onMessage(msg: EthernetConnector.ClientRequest): Behavior[EthernetConnector.ClientRequest] =
    msg match {
      case TransferCoins(publicKey, replyTo) =>
        val pattern = "0x[0-9a-fA-F]{40}".r
        val string = pattern.replaceFirstIn(publicKey.value, "")

        //check if public key has valid form
        if (publicKey == null || publicKey.value.isEmpty) replyTo ! InvalidPublicKey("empty public key")
        if (!string.isEmpty) {
          replyTo ! TransactionPage.InvalidPublicKey(publicKey.value)
          replyTo ! TransactionPage.ShutdownActor()
          Behaviors.same
        }
        //send request and send response to front end actor
        else {
            sendCoin(publicKey).onComplete {
            //JsonRPC response processing
            case Success(res) => {
              res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {
                body => {
                  try {
                    parseRespondJson(body.utf8String) match {
                      case JsonRPCSucces(jsonrpc, id, result) => {
                        //JsonRPC signals the transaction successful
                        replyTo ! TransactionPage.Success(TransactionHashCode(result))
                        replyTo ! TransactionPage.ShutdownActor()
                      }
                      case JsonRPCFail(jsonrpc, id, error) => {
                        //JsonRPC signals some errors
                        context.log.error(s"JsonRPC API signals error. Error message: ${error}")
                        replyTo ! TransactionPage.JsonRPCError(error.message)
                        replyTo ! TransactionPage.ShutdownActor()
                      }
                    }
                  }
                  catch {
                    case x: RuntimeException => {
                      context.log.error(s"JsonRPC API response deserialization error. Exception msg: ${x.getMessage}")
                      replyTo ! TransactionPage.ServerError("Internal server error")
                      replyTo ! TransactionPage.ShutdownActor()
                    }
                  }
                }
              }
            }
              Behaviors.same
            case Failure(exception) => {
              //Some problem with connection with JsonRPC Api
              context.log.error(s"Error with connection with testnet rinkeby. Exception message: ${exception.getMessage}")
              replyTo ! TransactionPage.ServerError("Internal server error")
              replyTo ! TransactionPage.ShutdownActor()
            }
          }
          Behaviors.same
        }
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


  override def sendCoin(publicKey: PublicKey): Future[HttpResponse] = {

    /*
        build request to JsonRPC api server
         */
    val params =
      List(Map(
        "from" -> config.getString("rinkeby.account.public-key"),
        "to" -> publicKey.value,
        "gas" -> "0x".concat(config.getInt("rinkeby.gas-limit").toHexString),
        "gasPrice" -> "0x".concat(config.getInt("rinkeby.gas-price").toHexString),
        "value" -> "0x".concat(config.getInt("rinkeby.value").toHexString),
        "data" -> "0x0a"))

    val req = JsonRPCSendCoin("2.0", "eth_sendTransaction", params, 1)
    context.log.debug(s"Attemption to send Ether to ${publicKey.value}")

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "http://" + config.getString("http.connecting.addres") + ":" + config.getString("http.connecting.port"),
      entity = HttpEntity(ContentTypes.`application/json`, req.toJson.toString())
    )


    Http().singleRequest(request)
  }


  // With regex find matching patter for jsonrpc respond (naive)
  @throws[RuntimeException]
  private def parseRespondJson(answer: String): JsonRPCResponseModel = {

    //succes respond include 'result':something part
    val successPattern = "\"result\"\\s*:".r
    //error pattern
    val errorPattern = "\"error\"\\s*:".r

    //if true it's succesful transactin communicate
    if (errorPattern.replaceFirstIn(answer, "").equals(answer)) {
      Unmarshal(answer).to[JsonRPCSucces].value.get.get
    }
    else {
      //else fail jsonrpc json
      Unmarshal(answer).to[JsonRPCFail].value.get.get
    }
  }
}

