package sample.hellopond

import scala.collection.immutable.{Seq => ISeq}

// Fish that reacts to CounterAddCommands and CounterEvents
case class CounterEvent(override val timestamp: Int, override val source: String, value: Int)
    extends Event(timestamp, source)

case class CounterAddCommand(value: Int) extends Command

object CounterFish {
  // when CounterEvent comes, add the value contained in it to the state
  def onEvent(e: Event, state: Int): Int = {
    e match {
      case CounterEvent(_, _, value) =>
        println(s"counterFish, will return state of ${state + value}")
        return state + value
      case other => println(s"unrecognized message: $other")
    }
    return state
  }

  // when CounterAddCommand comes, pass the value in an event
  def onCommand(c: Command, state: Int): ISeq[Event] = {
    c match {
      case CounterAddCommand(value) => return Vector(CounterEvent((System.currentTimeMillis() / 100000).toInt, "counterFish", value))
      case _ =>
    }
    Vector.empty
  }
  def initialState: Int = 0
}
