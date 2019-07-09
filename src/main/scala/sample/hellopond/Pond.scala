package sample.hellopond

import akka.actor.Actor
import scala.collection.immutable.TreeMap
import scala.collection.immutable.Set
import scala.collection.immutable.{Seq => ISeq}
import scala.collection.mutable.ArrayBuffer


class Event(val timestamp: Int, val source: String)

case class TerminateEvent(override val timestamp: Int, override val source: String) extends Event(timestamp, source)

final case class EventEnvelope(topic: String, payload: Event)

class Command

trait Fish[S] { // to have a list of those fishes
    def onEvent(e: Event, state: S): S
    def onCommand(c: Command, state: S): ISeq[Event]
    def initialState: S
}

final case class ConnectToBus(b: LookupBusImpl, topic: String)

final case class AddFish(f: FishJar[Any])

final case class Disconnect()

final case class Reconnect()

class Pond extends Actor {
    // disconnect
    // connect
    // needs buffer for outbound messages; that will be sent only if the pond is connected
    // there will be event tree map and stuff will be emitted from the timestamp of currently added events,
    // if this results in replay then the states will be overwritten
  var lookupBus: LookupBusImpl = null
  var events: TreeMap[Int, Set[Event]] = TreeMap() // map goes from timestamps to events
  var states: Vector[Vector[_]] = Vector() // this a thing to keep states in
  var emissions: Vector[Event] = Vector()
  var connected: Boolean = true
  var fishes: Vector[FishJar[_]] = Vector()
  def receive = {
    case cb: ConnectToBus =>
        lookupBus = cb.b
        lookupBus.subscribe(self, cb.topic)
    case Disconnect =>
        println("Disconnecting")
        connected = false
    case Reconnect =>
        println("Reconnecting")
        connected = true
        emissions.foreach(em => lookupBus.publish(EventEnvelope("greetings", em)))
        emissions = Vector()
    case AddFish(fish) =>
        println("Adding a fish")
        fishes = fishes :+ fish
        var newStates: Vector[Vector[_]] = Vector()
        for (i <- 0 until states.length) {
            val state = states(i) :+ fish.fish.initialState
            newStates = newStates :+ state
        }
        states = newStates
    case TerminateEvent(_, _) =>
        println("Terminating")
        context.system.terminate()
    case e: Event =>
      println("Adding event to its place in the map")
      val t: Int = e.timestamp
      events = if(events.contains(t)) {
          val nset: Set[Event] = events(t) + e
          events + (t -> nset)
      } else {
          events + (t -> Set(e))
      }
      val evSeq: Seq[(Int, Event)] = events.foldLeft (Seq.empty[(Int, Event)]) { (acc: Seq[(Int, Event)], elem: (Int, Set[Event])) => acc ++ elem._2.map(el => (elem._1, el)).toSeq }
      val evInd = evSeq.indexWhere(_._1 >= t)
      println(s"Forwarding event to fishes, replaying from $evInd to ${evSeq.length}")
      var newState: Vector[_] = Vector()
      var curState: Vector[_] = if (evInd > 0) {
        //   println(s"states length: ${states.length} $evInd ${states(evInd - 1).length}")
          states(evInd - 1)
      } else {
        //   println("will try with fishes.map")
          fishes.map(fj => fj.fish.initialState)
      }
    //   println(s"curState lenght ${curState.length}")
    //   println("indexes: ", evInd, evSeq.length)
      evSeq.slice(evInd, evSeq.length).zipWithIndex.foreach{
        case (ev, index) => {
            println("iterating")
                fishes.zipWithIndex.foreach { case (fj, ind) => {
                    val nState = fj.onEvent(ev._2, fj.toFishState(curState(ind)))
                    newState = newState :+ nState
                }
            }
        }
        curState = newState.toVector
        // println("curState length 2 ", curState.length)
        if (states.length <= index + evInd) {
            // println("extending")
            states = states :+ curState
        } else {
            // println("updating")
            states = states.updated(index + evInd, curState)
        }
        // println(s"states length: ${states.length}, events length ${events.size}")
    }
    // println(s"end states length: ${states.length}, events length ${events.size}")
    println(s"end states length: ${states.length} events length ${events.size}")
    println(s"states table: ${states}")
    println(s"events table: ${events}")
    case c: Command =>
        val curState: Vector[_] = if (states.length > 0) {
            // println("1")
            states(states.length - 1)
        } else {
            // println("2")
            fishes.map(fj => fj.fish.initialState)
        }
        println(s"curState: $curState")
        fishes.zipWithIndex.foreach { case (fj, ind) => {
            val newEmissions = fj.onCommand(c, fj.toFishState(curState(ind)))
            if (connected) {
                newEmissions.foreach(em => lookupBus.publish(EventEnvelope("greetings", em)))
            } else {
                emissions = emissions ++ newEmissions
            }
        }}
    case str: String =>
      println("Some other message...", str)
  }
}
