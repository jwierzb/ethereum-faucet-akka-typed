/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.actor


import akka.actor.testkit.typed.Effect._
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox, TestProbe}
import akka.actor.typed._
import akka.actor.typed.scaladsl.{ActorContext, _}
import akka.event.Logging
import com.jwierzbicki.rinkebyfaucet.frontend.TransactionPage.{InvalidPublicKey, TransactionPageData}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.jwierzbicki.rinkebyfaucet.Json.JsonSupport
import com.jwierzbicki.rinkebyfaucet.actor.EthernetConnector.TransferCoins
import com.jwierzbicki.rinkebyfaucet.actor.Test.EthernetCommand
import com.jwierzbicki.rinkebyfaucet.frontend.TransactionPage
import com.jwierzbicki.rinkebyfaucet.model.EtherModel.{PublicKey, TransactionHashCode}
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}




class WithJsonRPCTest extends WordSpec with BeforeAndAfterAll  with MockFactory with Matchers {


  // important to provide actor system (untyped(implicit) wont work!)
  var testKit = ActorTestKit()

  "An ether actor" must {

    "return invalid public key response(non empty key)" in {

      implicit val ac = testKit.system

      val act = testKit.spawn(EthernetConnector.withJsonRPC())
      val probe = testKit.createTestProbe[TransactionPageData]()

      act ! TransferCoins(PublicKey("invalid"), probe.ref)
      probe.expectMessage(InvalidPublicKey("invalid"))

    }
    "return \"empty public key\" response (with empty key)" in {

      implicit val ac = testKit.system

      val act = testKit.spawn(EthernetConnector.withJsonRPC())
      val probe = testKit.createTestProbe[TransactionPageData]()

      act ! TransferCoins(PublicKey(""), probe.ref)
      probe.expectMessage(InvalidPublicKey("empty public key"))
    }

    "return JsonRPCError with \"error message\" message" in {
      import akka.actor.typed.scaladsl.adapter._

      implicit val ac = testKit.system
      implicit val exec = ac.executionContext
      implicit val actorSystem = ac.toUntyped


      val response: HttpResponse = HttpResponse(StatusCodes.OK, entity = HttpEntity.apply(ContentTypes.`application/json`, "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32000, \"message\":\"error message\"}}"))
      val act = testKit.spawn(Test.EthernetCommand.withJsonRPC((_, _) => Future {response}))

      val probe = testKit.createTestProbe[TransactionPageData]()

      act ! Test.EthernetCommand.TransferCoins(PublicKey("0x2f5c7f32666fcefd083a9e3c4fcb2d3f096089bf"), probe.ref)
      probe.expectMessage(TransactionPage.JsonRPCError("error message"))
    }

    "return Internal Server Error due to response unamrshaling exception" in {
      import akka.actor.typed.scaladsl.adapter._

      implicit val ac = testKit.system
      implicit val exec = ac.executionContext
      implicit val actorSystem = ac.toUntyped


      //response is corrupted jsonrpc api server response
      val response: HttpResponse = HttpResponse(StatusCodes.OK, entity = HttpEntity.apply(ContentTypes.`application/json`, "{\"onddrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32000, \"message\":\"no suitable peers available\"}}"))

      val act = testKit.spawn(Test.EthernetCommand.withJsonRPC((_, _) => Future {response}))
      val probe = testKit.createTestProbe[TransactionPageData]()

      act ! Test.EthernetCommand.TransferCoins(PublicKey("0x2f5c7f32666fcefd083a9e3c4fcb2d3f096089bf"), probe.ref)
      probe.expectMessage(TransactionPage.ServerError("Internal server error"))
    }


    "return Internal Server Error due to failed JsonRPC API response" in {
      import akka.actor.typed.scaladsl.adapter._

      implicit val ac = testKit.system
      implicit val exec = ac.executionContext
      implicit val actorSystem = ac.toUntyped

      val act = testKit.spawn(Test.EthernetCommand.withJsonRPC((_, _) => Future {throw new Exception("msg")}))
      val probe = testKit.createTestProbe[TransactionPageData]()

      act ! Test.EthernetCommand.TransferCoins(PublicKey("0x2f5c7f32666fcefd083a9e3c4fcb2d3f096089bf"), probe.ref)

      probe.expectMessage(TransactionPage.ServerError("Internal server error"))
    }
    "return hash of succesfull transaction" in {
      import akka.actor.typed.scaladsl.adapter._

      implicit val ac = testKit.system
      implicit val exec = ac.executionContext
      implicit val actorSystem = ac.toUntyped

      val response: HttpResponse = HttpResponse(StatusCodes.OK, entity = HttpEntity.apply(ContentTypes.`application/json`, "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"transaction hash\"}"))

      val act = testKit.spawn(Test.EthernetCommand.withJsonRPC((_, _) => Future { response}))
      val probe = testKit.createTestProbe[TransactionPageData]()

      act ! Test.EthernetCommand.TransferCoins(PublicKey("0x2f5c7f32666fcefd083a9e3c4fcb2d3f096089bf"), probe.ref)

      probe.expectMessage(TransactionPage.Success(TransactionHashCode("transaction hash")))
    }
  }

}




/**
  * Copy of original EthernetCommand but contructor of WithJsonRPC actor takes as parameter function
  * returning Future[HttpRepsonse] simulating JsonRPC API server responses (to stub it except sendCoin method)
  * due to problems with mocking actotr's methods (actor's class object reference is not available explicitly)
  */
object Test {
  /**
    * Actor responsible for sending requests to JsonRPC API, process response and send result
    * to another actor with ref placed in message.
    */
  trait EthernetCommandT extends AbstractBehavior[EthernetCommand.ClientRequest] {
    /**
      * Method sending transaction to wallet with @param publicKey through JsonRPC APi
      * and @return server response
      */

  }

  object EthernetCommand {

    def withJsonRPC(x: (PublicKey, ActorContext[EthernetCommand.ClientRequest])=>Future[HttpResponse]): Behavior[Test.EthernetCommand.ClientRequest] = Behaviors.setup(ctx => new WithJsonRPC(ctx, x))

    sealed trait ClientRequest
    final case class TransferCoins(publicKey: PublicKey, replyTo: ActorRef[TransactionPageData]) extends ClientRequest
  }

  class WithJsonRPC(val context: ActorContext[EthernetCommand.ClientRequest], func: (PublicKey, ActorContext[EthernetCommand.ClientRequest])=>Future[HttpResponse] ) extends EthernetCommandT with JsonSupport {

    import spray.json._
    import com.jwierzbicki.rinkebyfaucet.model._

    import akka.actor.typed.scaladsl.adapter._
    implicit val ac = context.system.toUntyped //untyped actor system
    implicit val ec = context.executionContext
    implicit val ma = ActorMaterializer.create(ac) //actor materializer (bytestrings)

    private lazy val config = ConfigFactory.load()

    import Test.EthernetCommand.TransferCoins

    override def onMessage(msg: EthernetCommand.ClientRequest): Behavior[EthernetCommand.ClientRequest] =
      msg match {
        case TransferCoins(publicKey, replyTo) =>
          val pattern = "0x[0-9a-fA-F]{40}".r
          val string = pattern.replaceFirstIn(publicKey.value, "")

          //check if public key has valid form
          if (publicKey == null || publicKey.value.isEmpty) replyTo ! InvalidPublicKey("empty public key")
          if (!string.isEmpty) {
            replyTo ! InvalidPublicKey(publicKey.value)
          }
          //send request and send response to front end
          else {
            val response = func(publicKey, context)
            response.onComplete {
              //JsonRPC response processing
              case Success(res) => {
                res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {
                  body => {
                    try {
                      parseRespondJson(body.utf8String) match {
                        case JsonRPCSucces(jsonrpc, id, result) => {
                          //JsonRPC signals the transaction successful
                          replyTo ! TransactionPage.Success(TransactionHashCode(result))
                        }
                        case JsonRPCFail(jsonrpc, id, error) => {
                          //JsonRPC signals some errors
                          context.log.error(s"JsonRPC API signals error. Error message: ${error}")
                          replyTo ! TransactionPage.JsonRPCError(error.message)
                        }
                      }
                    }
                    catch {
                      case x: RuntimeException => {
                        context.log.error(s"JsonRPC API response deserialization error. Exception msg: ${x.getMessage}")
                        replyTo ! TransactionPage.ServerError("Internal server error")
                      }
                    }
                  }
                }
              }
              case Failure(exception) => {
                //Some problem with connection with JsonRPC Api
                context.log.error(s"Error with connection with testnet rinkeby. Exception message: ${exception.getMessage}")
                replyTo ! TransactionPage.ServerError("Internal server error")
              }
            }
          }
          this
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

}
