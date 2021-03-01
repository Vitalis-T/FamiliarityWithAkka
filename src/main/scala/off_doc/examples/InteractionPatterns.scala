package off_doc.examples
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps

object Printer {
	case class PrintMe(message: String)

  def apply(): Behavior[PrintMe] =
    Behaviors.receive {
      case (context, PrintMe(message)) =>
        context.log.info(message)
        Behaviors.same
    }
}

object CookieFabric {
	final case class Request(query: String, replyTo: ActorRef[Response])
	final case class Response(result: String)

	def apply(): Behaviors.Receive[Request] =
  Behaviors.receiveMessage[Request] {
    case Request(query, replyTo) =>
      // ... process query ...
      replyTo ! Response(s"Here are the cookies for [$query]!")
      Behaviors.same
  }
}

/*object InteractionTest extends App {
	val system = ActorSystem(Printer(), "fire-and-forget-sample")
	// note how the system is also the top level actor ref
	val printer: ActorRef[Printer.PrintMe] = system
	// these are all fire and forget
	printer ! Printer.PrintMe("message 1")
	printer ! Printer.PrintMe("not message 2")
}*/
