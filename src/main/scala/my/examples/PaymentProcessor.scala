package my.examples
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import java.lang.module.FindException

sealed trait ConfigurationMessage
final case class RetrieveConfiguration(merchantId: MerchantId, replyTo: ActorRef[ConfigurationResponse]) extends ConfigurationMessage
 
sealed trait ConfigurationResponse
final case class ConfigurationFound(merchantId: MerchantId,  merchantConfiguration: MerchantConfiguration) extends ConfigurationResponse
final case class ConfigurationNotFound(merchanId: MerchantId) extends ConfigurationResponse
 
case class MerchantId(id: String) extends AnyVal
case class BankIdentifier(id: String) extends AnyVal
 
case class MerchantConfiguration(bankIdentifier: BankIdentifier)

// the AbstractBehavior trait is the entry point for using the object-oriented style API
class Configuration(context: ActorContext[ConfigurationMessage]) extends AbstractBehavior[ConfigurationMessage](context) {
 
  // the mutable state here holds the configuration values of each merchant we know about
  var configurations: Map[MerchantId, MerchantConfiguration] = Map.empty
 
  // the onMessage method defines the initial behavior applied to a message upon reception
  override def onMessage(msg: ConfigurationMessage): Behavior[ConfigurationMessage] = msg match {
    case RetrieveConfiguration(merchantId, replyTo) =>
      configurations.get(merchantId) match {
        case Some(configuration) =>
          // reply to the sender using the fire-and-forget paradigm
          replyTo ! ConfigurationFound(merchantId, configuration)
        case None =>
          // reply to the sender using the fire-and-forget paradigm
          replyTo ! ConfigurationNotFound(merchantId)
      }
      // lastly, return the Behavior to be applied to the next received message
      // in this case, that's just the same Behavior as we already have
      this
  }
}

object PaymentProcessor {
 def apply() = Behaviors.setup[Nothing] { context =>
   context.log.info("Typed Payment Processor started")
   context.spawn(Configuration(), "config")
   Behaviors.empty
  }
 }
  
object Configuration {
 def apply(): Behavior[ConfigurationMessage] = Behaviors.setup(context => new Configuration(context))
}
  
object Main extends App {
 val root = ActorSystem[Nothing](PaymentProcessor(), "typed-payment-processor")
 Thread.sleep(1000)
 root.terminate()
}