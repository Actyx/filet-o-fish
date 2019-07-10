package sample.hellopond

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Terminated

object Main {

  def main(args: Array[String]): Unit = {
    // create a new actor system
    val system = ActorSystem("Hello")

    // wire two ponds
    val a = system.actorOf(Props[Pond], "Alpha")
    val b = system.actorOf(Props[Pond], "Beta")

    // create a new messageBus
    val messageBus = new LookupBusImpl

    // ask the two created ponds to connect to it (channel: greetings)
    a ! ConnectToBus(messageBus, "greetings")
    b ! ConnectToBus(messageBus, "greetings")

    // create two fishes (please note that our fishes are stateless, so can be reused)
    val counterFish: Fish[Int] = Fish[Int](CounterFish.onEvent, CounterFish.onCommand, CounterFish.initialState)
    val stringFish: Fish[String] = Fish[String](StringFish.onEvent, StringFish.onCommand, StringFish.initialState)

    // and add them to the ponds
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
    // messageBus.publish(EventEnvelope("time", StringEvent(1, "timeSource", System.currentTimeMillis().toString())))
    // messageBus.publish(EventEnvelope("greetings", StringEvent(2, "source1", "hello")))
    // messageBus.publish(EventEnvelope("greetings", CounterEvent(4, "source1", 1)))
    // messageBus.publish(EventEnvelope("greetings", CounterEvent(5, "source1", 2)))
    // messageBus.publish(EventEnvelope("greetings", CounterEvent(3, "source1", 3)))
    a ! TerminateEvent()
  }
}
