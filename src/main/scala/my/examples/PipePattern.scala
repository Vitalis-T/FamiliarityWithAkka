package my.examples
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import scala.concurrent.Future
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.util.Success
import scala.util.Failure
import akka.actor.typed.ActorSystem
import java.lang.module.FindException

object TypedPipePattern {

  object Infrastructure {
    private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(6))
    private val db: Map[String, Int] = Map(
      "Vitalii" -> 123,
      "Anton" -> 456,
      "Piston" -> 789)

    def asynRetrievePhonNumber(name: String): Future[Int] = 
      Future(db(name))
  }

  trait PhoneCallProtocol
  case class FindAndCall(name: String) extends PhoneCallProtocol
  case class InitiatePhoneCall(number: Int) extends PhoneCallProtocol
  case class LogPhoneCallFailure(cause: Throwable) extends PhoneCallProtocol 

  val phoneCallInitiator1: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
    var nPhoneCalls = 0
    var nFailures = 0
    implicit val ec: ExecutionContext = context.executionContext

    Behaviors.receiveMessage {
      case FindAndCall(name) =>
        val futureNumber = Infrastructure.asynRetrievePhonNumber(name)
        futureNumber.onComplete {
          case Success(number) =>
            context.log.info(s"Initiating phone call for $number")
            nPhoneCalls += 1
          case Failure(ex) =>
            context.log.error(s"Phone call failed for $name : $ex")
            nFailures += 1
        }
      Behaviors.same
    }
  }
  //pipe pattern = forward the result of future back to me as message
  val phoneCallInitiator2: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
    var nPhoneCalls = 0
    var nFailures = 0
    implicit val ec: ExecutionContext = context.executionContext

    Behaviors.receiveMessage {
      case FindAndCall(name) =>
        val futureNumber = Infrastructure.asynRetrievePhonNumber(name)
        context.pipeToSelf(futureNumber) {
          case Success(number) => InitiatePhoneCall(number)
          case Failure(ex) => LogPhoneCallFailure(ex)
        }
      Behaviors.same

      case InitiatePhoneCall(number) =>
        context.log.info(s"Initiating phone call for $number")
        nPhoneCalls += 1
        Behaviors.same

      case LogPhoneCallFailure(ex) =>
        context.log.error(s"Phone call failed $ex")
        nFailures += 1
        Behaviors.same
    }
  }

  def phoneCallInitiator3(nPhoneCalls: Int = 0, nFailures: Int = 0): Behavior[PhoneCallProtocol] = 
    Behaviors.receive { (context, message) =>
    message match {
      case FindAndCall(name) =>
        val futureNumber = Infrastructure.asynRetrievePhonNumber(name)
        context.pipeToSelf(futureNumber) {
          case Success(number) => InitiatePhoneCall(number)
          case Failure(ex) => LogPhoneCallFailure(ex)
        }
      Behaviors.same

      case InitiatePhoneCall(number) =>
        context.log.info(s"Initiating phone call for $number")
        phoneCallInitiator3(nPhoneCalls + 1, nFailures)

      case LogPhoneCallFailure(ex) =>
        context.log.error(s"Phone call failed $ex")
        phoneCallInitiator3(nPhoneCalls, nFailures + 1)
    }

  }


  def main(args: Array[String]): Unit = {
    val root = ActorSystem(phoneCallInitiator3(), "PhoneCaller")
    root ! FindAndCall("Vitalii")
    root ! FindAndCall("Anton")
    root ! FindAndCall("Akka")

    Thread.sleep(1000)
    root.terminate()
  }

}