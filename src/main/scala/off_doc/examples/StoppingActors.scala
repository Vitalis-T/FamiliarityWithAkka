package off_doc.examples
import scala.concurrent.Await
import scala.concurrent.duration._
import org.slf4j.Logger
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps._
import akka.actor.typed.{ ActorSystem, PostStop }

object MasterControlProgram {
  sealed trait Command
  final case class SpawnJob(name: String) extends Command
  case object GracefulShutdown extends Command

  // Predefined cleanup operation
  def cleanup(log: Logger): Unit = log.info("Cleaning up!")

  def apply(): Behavior[Command] = {
    Behaviors
      .receive[Command] { (context, message) =>
        message match {
          case SpawnJob(jobName) =>
            context.log.info("Spawning job {}!", jobName)
            context.spawn(Job(jobName), name = jobName)
            Behaviors.same
          case GracefulShutdown =>
            context.log.info("Initiating graceful shutdown...")
            // perform graceful stop, executing cleanup before final system termination
            // behavior executing cleanup is passed as a parameter to Actor.stopped
            Behaviors.stopped { () =>
              cleanup(context.system.log)
            }
        }
      }
      .receiveSignal {
        case (context, PostStop) =>
          context.log.info("Master Control Program stopped")
          Behaviors.same
      }
  }
}

object MasterControlProgram2 {
  sealed trait Command
  final case class SpawnJob(name: String, replyToWhenDone: ActorRef[JobDone]) extends Command
  final case class JobDone(name: String)
  private final case class JobTerminated(name: String, replyToWhenDone: ActorRef[JobDone]) extends Command

  def apply(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case SpawnJob(jobName, replyToWhenDone) =>
          context.log.info("Spawning job {}!", jobName)
          val job = context.spawn(Job(jobName), name = jobName)
          context.watchWith(job, JobTerminated(jobName, replyToWhenDone))
          Behaviors.same
        case JobTerminated(jobName, replyToWhenDone) =>
          context.log.info("Job stopped: {}", jobName)
          replyToWhenDone ! JobDone(jobName)
          Behaviors.same
      }
    }
  }
}

object Job {
  sealed trait Command

  def apply(name: String): Behavior[Command] = {
    Behaviors.receiveSignal[Command] {
      case (context, PostStop) =>
        context.log.info("Worker {} stopped", name)
        Behaviors.same
    }
  }
}

object StoppingTest extends App {
	import MasterControlProgram._
	val system: ActorSystem[MasterControlProgram.Command] = ActorSystem(MasterControlProgram(), "B7700")

	system ! SpawnJob("a")
	system ! SpawnJob("b")
	Thread.sleep(1000)
	// gracefully stop the system
	system ! GracefulShutdown
	Thread.sleep(100)
	Await.result(system.whenTerminated, 3.seconds)
}