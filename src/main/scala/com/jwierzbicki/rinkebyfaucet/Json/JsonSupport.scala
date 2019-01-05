/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.Json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.jwierzbicki.rinkebyfaucet.model.{JsonRPCFail,JsonRPCError, JsonRPCMethod, JsonRPCSendCoin, JsonRPCSucces}
import spray.json.DefaultJsonProtocol

/**
  * Class providing function relevant in model json processing
  */
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val jsonRPCMethodFormat = jsonFormat4(JsonRPCMethod)
  implicit val jsonRPCSuccesFormat = jsonFormat3(JsonRPCSucces)
  implicit val jsonRPCerrorFormat = jsonFormat2(JsonRPCError)
  implicit val jsonRPCFailFormat = jsonFormat3(JsonRPCFail)
  implicit val jsonRPCSendCoin = jsonFormat4(JsonRPCSendCoin)

}
