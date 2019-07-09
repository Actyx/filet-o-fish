package sample.hellopond

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Terminated
import akka.event.EventBus
import akka.event.LookupClassification

/**
 * Publishes the payload of the EventEnvelope when the topic of the
 * EventEnvelope equals the String specified when subscribing.
 */
class LookupBusImpl extends EventBus with LookupClassification {
  type Event = EventEnvelope
  type Classifier = String
  type Subscriber = ActorRef

  // is used for extracting the classifier from the incoming events
  override protected def classify(event: Event): Classifier = event.topic

  // will be invoked for each event for all subscribers which registered themselves
  // for the event’s classifier
  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event.payload
  }

  // must define a full order over the subscribers, expressed as expected from
  // `java.lang.Comparable.compare`
  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int =
    a.compareTo(b)

  // determines the initial size of the index data structure
  // used internally (i.e. the expected number of different classifiers)
  override protected def mapSize: Int = 128

}

object Main {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Hello")
    val a = system.actorOf(Props[Pond], "helloWorld")
    val b = system.actorOf(Props[Pond], "helloWorld2")
    val lookupBus = new LookupBusImpl
    lookupBus.subscribe(a, "greetings")
    lookupBus.subscribe(b, "greetings")
    a ! ConnectToBus(lookupBus)
    b ! ConnectToBus(lookupBus)
    val counterFish: CounterFish = new CounterFish()
    val stringFish: StringFish = new StringFish()
    a ! AddFish(new FishJar[Int](counterFish))
    b ! AddFish(new FishJar[Int](counterFish))
    a ! AddFish(new FishJar[String](stringFish))
    // a ! Disconnect
    a ! CounterAddCommand(3)
    Thread.sleep(3000)
    b ! CounterAddCommand(2)
    Thread.sleep(3000)
    // a ! Reconnect
    Thread.sleep(3000)
    // lookupBus.publish(EventEnvelope("time", StringEvent(1, "timeSource", System.currentTimeMillis().toString())))
    // lookupBus.publish(EventEnvelope("greetings", StringEvent(2, "source1", "hello")))
    // lookupBus.publish(EventEnvelope("greetings", CounterEvent(4, "source1", 1)))
    // lookupBus.publish(EventEnvelope("greetings", CounterEvent(5, "source1", 2)))
    // lookupBus.publish(EventEnvelope("greetings", CounterEvent(3, "source1", 3)))
    lookupBus.publish(EventEnvelope("greetings", TerminateEvent(6, "grimReaper")))
  }
}
