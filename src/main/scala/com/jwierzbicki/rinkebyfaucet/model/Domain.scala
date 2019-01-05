/*
 * Made by Jakub Wierzbicki @jwierzb
 */

/*
 * Made by Jakub Wierzbicki @jwierzb
 */

package com.jwierzbicki.rinkebyfaucet.model


/*
Representation of chainblock entities
 */
object EtherModel{
  final case class TransactionHashCode(value: String)
  final case class PublicKey(value: String)
}


/*
Model for constructing http requests into objects
 */
trait EndpointRequestModel
final case class TransferRequest(key: EtherModel.PublicKey) extends EndpointRequestModel



/*
Model for constructing requests to JsonRPC API
 */
sealed trait JsonRPCRequestModel

final case class JsonRPCMethod(jsonrpc: String, method: String, params: List[String], id: Int) extends JsonRPCRequestModel

final case class JsonRPCSendCoin(jsonrpc: String, method: String, params: List[Map[String, String]], id: Int) extends JsonRPCRequestModel


/*
Model for constructing responses returned by JsonRPC API
 */
sealed trait JsonRPCResponseModel

final case class JsonRPCSucces(jsonrpc: String, id: Int, result: String) extends JsonRPCResponseModel

final case class JsonRPCError(code: Int, message: String)

final case class JsonRPCFail(jsonrpc: String, id: Int, error: JsonRPCError) extends JsonRPCResponseModel
