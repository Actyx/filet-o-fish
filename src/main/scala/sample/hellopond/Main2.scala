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
    val producer: Fish[ProducerState] = Fish[ProducerState](ProducerFish.onEvent, ProducerFish.onCommand, ProducerFish.initialState)
    val consumerA: Fish[ConsumerState] = Fish[ConsumerState](ConsumerA.onEvent, ConsumerA.onCommand, ConsumerA.initialState)
    val consumerB: Fish[ConsumerState] = Fish[ConsumerState](ConsumerB.onEvent, ConsumerB.onCommand, ConsumerB.initialState)

    // and add them to the ponds
    p ! AddFish(new FishJar[ProducerState](producer))
    c1 ! AddFish(new FishJar[ConsumerState](consumerA))
    c2 ! AddFish(new FishJar[ConsumerState](consumerB))
    
    p ! ProduceCommand()

    Thread.sleep(2000)

    // c2 ! Disconnect()

    c1 ! ConsumeCommand()

    Thread.sleep(500)

    c2 ! ConsumeCommand()

    Thread.sleep(500)

    // c2 ! Reconnect()

    Thread.sleep(3000)

    c1 ! DumpFishStates()
    c2 ! DumpFishStates()
    p ! DumpFishStates()

    Thread.sleep(500)

    p ! TerminateEvent()
  }
}
