package off_doc.examples

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import akka.actor.typed.Behavior
import scala.util.Success
import scala.util.Failure
import scala.concurrent.duration._

object Hal {
  sealed trait Command
  case class OpenThePodBayDoorsPlease(replyTo: ActorRef[Response]) extends Command
  case class Response(message: String)

  def apply(): Behaviors.Receive[Hal.Command] =
    Behaviors.receiveMessage[Command] {
      case OpenThePodBayDoorsPlease(replyTo) =>
        replyTo ! Response("I'm sorry, Dave. I'm afraid I can't do that.")
        Behaviors.same
    }
}

object Dave {

  sealed trait Command
  // this is a part of the protocol that is internal to the actor itself
  private case class AdaptedResponse(message: String) extends Command

  def apply(hal: ActorRef[Hal.Command]): Behavior[Dave.Command] =
    Behaviors.setup[Command] { context =>
      // asking someone requires a timeout, if the timeout hits without response
      // the ask is failed with a TimeoutException
      implicit val timeout: Timeout = 3.seconds

      // Note: The second parameter list takes a function `ActorRef[T] => Message`,
      // as OpenThePodBayDoorsPlease is a case class it has a factory apply method
      // that is what we are passing as the second parameter here it could also be written
      // as `ref => OpenThePodBayDoorsPlease(ref)`
      context.ask(hal, Hal.OpenThePodBayDoorsPlease) {
        case Success(Hal.Response(message)) => AdaptedResponse(message)
        case Failure(_)                     => AdaptedResponse("Request failed")
      }

      // we can also tie in request context into an interaction, it is safe to look at
      // actor internal state from the transformation function, but remember that it may have
      // changed at the time the response arrives and the transformation is done, best is to
      // use immutable state we have closed over like here.
      val requestId = 1
      context.ask(hal, Hal.OpenThePodBayDoorsPlease) {
        case Success(Hal.Response(message)) => AdaptedResponse(s"$requestId: $message")
        case Failure(_)                     => AdaptedResponse(s"$requestId: Request failed")
      }

      Behaviors.receiveMessage {
        // the adapted message ends up being processed like any other
        // message sent to the actor
        case AdaptedResponse(message) =>
          context.log.info("Got response from hal: {}", message)
          Behaviors.same
      }
    }
}