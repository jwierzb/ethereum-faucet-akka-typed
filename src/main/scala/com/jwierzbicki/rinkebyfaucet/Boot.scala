import akka.actor.typed.ActorSystem
import com.jwierzbicki.rinkebyfaucet.http.Server


//Main object starting application
object Boot extends App {

    override def main(args: Array[String]): Unit = {
        ActorSystem[Nothing](Server(), "rinkeby-faucet")
    }
}