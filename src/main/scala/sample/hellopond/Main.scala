package sample.hellopond

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Terminated

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
