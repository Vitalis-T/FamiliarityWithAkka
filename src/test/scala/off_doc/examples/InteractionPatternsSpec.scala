package off_doc.examples

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.testkit.typed.scaladsl.LogCapturing
import akka.actor.typed.scaladsl.Behaviors
import java.net.URI

class InteractionPatternsSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with LogCapturing {
	"The interaction patterns docs" must {
		"contain a sample for ask" in {
			// somewhat modified behavior to let us know we saw the two requests
	    val monitor = createTestProbe[Hal.Command]()
	    val hal = spawn(Behaviors.monitor(monitor.ref, Hal()))
	    spawn(Dave(hal))
	    monitor.expectMessageType[Hal.OpenThePodBayDoorsPlease]
	    monitor.expectMessageType[Hal.OpenThePodBayDoorsPlease]
		}
	}

		"contain a sample for adapted response" in {
			val backend = spawn(Behaviors.receiveMessage[Backend.Request] {
	        case Backend.StartTranslationJob(taskId, _, replyTo) =>
	          replyTo ! Backend.JobStarted(taskId)
	          replyTo ! Backend.JobProgress(taskId, 0.25)
	          replyTo ! Backend.JobProgress(taskId, 0.50)
	          replyTo ! Backend.JobProgress(taskId, 0.75)
	          replyTo ! Backend.JobCompleted(taskId, new URI("https://akka.io/docs/sv/"))
	          Behaviors.same
	      })

	      val frontend = spawn(Frontend(backend))
	      val probe = createTestProbe[URI]()
	      frontend ! Frontend.Translate(new URI("https://akka.io/docs/"), probe.ref)
	      probe.expectMessage(new URI("https://akka.io/docs/sv/"))
	    }
}