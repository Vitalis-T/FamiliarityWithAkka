package my.examples
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import java.lang.module.FindException
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.Signal
import akka.actor.typed.PostStop
import akka.actor.typed.PreRestart

object NameCommand {
  def apply(): Behavior[String] = 
    Behaviors.setup(context => new NameCommand(context))
}

class NameCommand(context: ActorContext[String]) extends AbstractBehavior[String](context) {

  private val child = context.spawn(
    Behaviors.supervise(SupervisedActor()).onFailure(SupervisorStrategy.restart), name = "sypervised-actor")

  override def onMessage(msg: String): Behavior[String] = msg match {
    case "Vitali" => println(s"Piston: $msg")
      this
    case "Anton" => println(s"Zhyk: $msg")
      this 
    case "faildChild" => 
      child ! "fail"
      this
  }
}

object SupervisedActor {
  def apply(): Behavior[String] = 
    Behaviors.setup(context => new SypervisedActor(context))
}

class SypervisedActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("supervised actor started")

  override def onMessage(msg: String): Behavior[String] = msg match {
    case "fail" =>
      println("supervised actor is fails now")
      throw new Exception("I failed!") 
  }

  override def onSignal: PartialFunction[Signal,Behavior[String]] = {
    case PreRestart =>
      println("supervised actor will be restarted")
      this
    case PostStop =>
      println("supervised actor stopped")
      this
  }
}

object NameTest extends App {
  val testName = ActorSystem(NameCommand(), "testName")
  testName ! "Vitali"
  testName ! "Anton"
  testName ! "faildChild"
  Thread.sleep(1000)
  testName.terminate()
}