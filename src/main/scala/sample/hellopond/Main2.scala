package sample.hellopond

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Terminated

object Main2 {

  def main(args: Array[String]): Unit = {
    // create a new actor system
    val system = ActorSystem("Hello")

    // wire three ponds
    val p = system.actorOf(Props[Pond], "Producer")
    val c1 = system.actorOf(Props[Pond], "ConsumerA")
    val c2 = system.actorOf(Props[Pond], "ConsumerB")

    // create a new messageBus
    val messageBus = new LookupBusImpl

    // ask the three created ponds to connect to it (channel: greetings)
    p ! ConnectToBus(messageBus, "greetings")
    c1 ! ConnectToBus(messageBus, "greetings")
    c2 ! ConnectToBus(messageBus, "greetings")

    // create the fishes
    val producer: ProducerFish = new ProducerFish()
    val consumerA: ConsumerA = new ConsumerA
    val consumerB: ConsumerB = new ConsumerB

    // and add them to the ponds
    p ! AddFish(new FishJar[ProducerState](producer))
    c1 ! AddFish(new FishJar[ConsumerState](consumerA))
    c2 ! AddFish(new FishJar[ConsumerState](consumerB))
    
    p ! ProduceCommand(true)

    Thread.sleep(1000)

    // c2 ! Disconnect()

    c1 ! ConsumeCommand(true)

    Thread.sleep(500)

    c2 ! ConsumeCommand(true)

    Thread.sleep(500)

    // c2 ! Reconnect()

    Thread.sleep(3000)

    p ! TerminateEvent()
  }
}
