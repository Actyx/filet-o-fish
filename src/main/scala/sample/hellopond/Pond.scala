package sample.hellopond

import akka.actor.Actor
import scala.collection.immutable.TreeMap
import scala.collection.immutable.Set
import scala.collection.mutable.ArrayBuffer
import akka.event.Logging

// this event should be used at the end of the program to terminate the actor system
// and with it - the whole program
case class TerminateEvent()

// Pond level envelope to wrap Events
final case class EventEnvelope(topic: String, payload: Event)

// config wiring message asking the pond listen (and send) on a given topic
final case class ConnectToBus(b: LookupBusImpl, topic: String)

// temporarily disconnect from the topic - used to simulate network failure
final case class Disconnect()

// reconnect to a topic (sending all pending messages)
final case class Reconnect()

// config wiring message - add a fish (passed as a parameter, wrapped in a jar container)
// to a given pond
final case class AddFish(f: FishJar[Any])

// dump the state of all fishes
final case class DumpFishStates()

class Pond extends Actor {
  // for actorLogging of messages
  val log = Logging(context.system, this)

  // bus to send messages to (connection to data backplane)
  var lookupBus: LookupBusImpl = null

  // all events that this pond saw, sorted by timestamps
  var events: TreeMap[Int, Set[Event]] = TreeMap() // map goes from timestamps to events

  // all the states of all the fishes contained in this pond, used for the time travel algorithm
  var states: Vector[Vector[_]] = Vector()

  // container to hold events emitted by fishes from this pond temporarily
  // while the pond is disconnected
  var emissions: Vector[Event] = Vector()

  // container to hold incoming events while the pond is disconnected
  var incoming: Vector[Event] = Vector()

  // is the pond connected ?
  var connected: Boolean = true

  // all fishes swimming in this pond
  var fishes: Vector[FishJar[_]] = Vector()

  // handle config wiring messages
  def pondWiring: PartialFunction[Any, Unit] = {
    case cb: ConnectToBus =>
      lookupBus = cb.b
      lookupBus.subscribe(self, cb.topic)

    case Disconnect() =>
      log.info("Disconnecting")
      connected = false

    case Reconnect() =>
      log.info("Reconnecting")
      connected = true
      emissions.foreach(em => lookupBus.publish(EventEnvelope("greetings", em)))
      emissions = Vector()
      incoming.foreach(_ => self ! _)
      incoming = Vector()

    case AddFish(fish) =>
      log.info("Adding a fish to a pond")
      fishes = fishes :+ fish
      var newStates: Vector[Vector[_]] = Vector()
      for (i <- 0 until states.length) {
        val state = states(i) :+ fish.fish.initialState
        newStates = newStates :+ state
      }
      states = newStates

    case DumpFishStates() =>
      log.info("Dumping fish states upon request: {}", states)

    case TerminateEvent() =>
      log.info("Terminating the whole actor system")
      context.system.terminate()
  }

  // handler for Events
  // events are being received on the event bus
  def eventHandler: PartialFunction[Any, Unit] = {
    case e: Event =>
      if (!connected) { // if we are not connected, put it into a queue for later processing
        incoming :+ e
      } else {          // otherwise start processing
        val t: Int = e.timestamp

        // insert into treemap
        events = if (events.contains(t)) {
          val nset: Set[Event] = events(t) + e
          events + (t -> nset)
        } else {
          events + (t -> Set(e))
        }

        // temporarily convert the map to a sequence (list)
        val evSeq: Seq[(Int, Event)] = events.foldLeft(Seq.empty[(Int, Event)]) {
          (acc: Seq[(Int, Event)], elem: (Int, Set[Event])) =>
            acc ++ elem._2.map(el => (elem._1, el)).toSeq
        }

        // find the place from which we start time travel replay (usually the new event should be last)
        val evInd = evSeq.indexWhere(_._1 >= t)
        log.info(
          s"Forwarding event to fishes, replaying from $evInd to ${evSeq.length}"
        )

        // newly generated state
        var newState: Vector[_] = Vector()

        // state of old fishes corresponding to given event slot
        var curState: Vector[_] = if (evInd > 0) {
          states(evInd - 1)
        } else {
          fishes.map(fj => fj.fish.initialState)
        }

        // time travel algorithm - generate states for events and place them in the right slots
        evSeq.slice(evInd, evSeq.length).zipWithIndex.foreach {
          case (ev, index) => {
            fishes.zipWithIndex.foreach {
              case (fj, ind) => {
                val nState = fj.onEvent(ev._2, fj.toFishState(curState(ind)))
                newState = newState :+ nState
              }
            }
          }
          curState = newState.toVector
          if (states.length <= index + evInd) {
            states = states :+ curState
          } else {
            states = states.updated(index + evInd, curState)
          }
        }

        //   log.info(
        //     s"end states length: ${states.length} events length ${events.size}"
        //   )
        log.info("After time travel states table: {}", states)
        log.info("After time travel events table: {}", events)
      }
  }

  // handler for the commands, commands are received directly by the pond
  def commandHandler: PartialFunction[Any, Unit] = {
    case c: Command =>
      // the most recent state across all the fishes
      val curState: Vector[_] = if (states.length > 0) {
        states(states.length - 1)
      } else {
        fishes.map(fj => fj.fish.initialState)
      }

      log.info("Applying command to current state {}", curState)

      // apply the incoming command to all the fishes (only interested ones will respond)
      // and either send the emitted events to pubsub (if connected) or gather them for later
      fishes.zipWithIndex.foreach {
        case (fj, ind) => {
          val newEmissions = fj.onCommand(c, fj.toFishState(curState(ind)))
          if (connected) {
            newEmissions.foreach(
              em => lookupBus.publish(EventEnvelope("greetings", em))
            )
          } else {
            emissions = emissions ++ newEmissions
          }
        }
      }
  }

  def receive = pondWiring orElse eventHandler orElse commandHandler
}
