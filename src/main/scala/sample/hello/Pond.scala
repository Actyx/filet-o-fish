package sample.hellopond

import akka.actor.Actor
import scala.collection.immutable.TreeMap
import scala.collection.immutable.Set
import scala.collection.mutable.ArrayBuffer


class Event(val timestamp: Int, val source: String)
case class StringEvent(override val timestamp: Int, override val source: String, value: String) extends Event(timestamp, source)
case class CounterEvent(override val timestamp: Int, override val source: String, value: Int) extends Event(timestamp, source)
case class TerminateEvent(override val timestamp: Int, override val source: String) extends Event(timestamp, source)
final case class EventEnvelope(topic: String, payload: Event)

class Command
case class CounterAddCommand(value: Int) extends Command

// this is a type-carrying container for fishes, to store them in a type-variance allowing collection (e.g. Vector[FishJar[_]])
// please note that you need to inform the Jar of the type of the underlying fish, e.g. when your fish is Fish[Int]
// you do it as follows: val intFishJar = new FishJar[Int](intFish)
class FishJar[+S : Manifest](val fish: Fish[_]) {
    def onEvent[U >: S](e: Event, state: U): S = { // but you know, U is really S, :wink: :wink:
        fish match {
            case f: Fish[S] => state match {
                case s: S => return f.onEvent(e, s)
                case _ => throw new IllegalArgumentException("the state passed must be S !") // ain't gonna happen
            }
            case _ => throw new IllegalArgumentException("the contained fish was of wrong type")
        }
    }
    def onCommand[U >: S](c: Command, state: U): Seq[Event] = { // but you know, U is really S, :wink: :wink:
        fish match {
            case f: Fish[S] => state match {
                case s: S => return f.onCommand(c, s)
                case _ => throw new IllegalArgumentException("the state passed must be S !") // ain't gonna happen
            }
            case _ => throw new IllegalArgumentException("the contained fish was of wrong type")
        }
    }
    def toFishState(state: Any): S = { // converter from Any
        state match {
            case s: S => return s
            case _ => throw new IllegalArgumentException("the state passed must be S !")
        }
    }
}

trait Fish[S] { // to have a list of those fishes
    def onEvent(e: Event, state: S): S
    def onCommand(c: Command, state: S): Seq[Event]
    def initialState: S
}

final case class ConnectToBus(b: LookupBusImpl)

final case class AddFish(f: FishJar[Any])

final case class Disconnect()

final case class Reconnect()

object Timestamp {
    var ts = 0
    def now(): Int = {
        ts += 1
        return ts
    }
}

// FIXME: the states table will be inside the fish, and instead of state, we will pass only state index, that will be validated
// the same would go for commands; unrecognized something is removed, obviously we react only to something we like
// FIXME: incremental generator for timestamps; maybe add a multimap, so that you can have duplicate timestamps

class CounterFish extends Fish[Int] {
    def onEvent(e: Event, state:Int): Int = {
        e match {
            case CounterEvent(_, _, value) =>
                println(s"counterFish, will return state of ${state+value}")
                return state + value
            case other => println(s"unrecognized message: $other")
        }
        return state
    }
    def onCommand(c: Command, state: Int): Seq[Event] = {
        c match {
            case CounterAddCommand(value) => return Vector(CounterEvent(Timestamp.now(), "counterFish", value))
            case _ =>
        }
        return Vector()
    }
    def initialState: Int = 0
}

class StringFish extends Fish[String] {
    def onEvent(e: Event, state: String): String = {
        e match {
            case StringEvent(_, _, str) => println(s"string $str")
            case other => println(s"stringFish unrecognized message $other")
        }
        return state
    }

    def onCommand(c: Command, state: String): Seq[Event] = {
        return Vector()
    }

    def initialState: String = ""
}

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
    case cb: ConnectToBus => lookupBus = cb.b
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
