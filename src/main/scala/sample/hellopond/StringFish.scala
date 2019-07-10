package sample.hellopond

import scala.collection.immutable.{Seq => ISeq}

// Fish that reacts to StringEvents, does not handle commands
case class StringEvent(override val timestamp: Int, override val source: String, value: String)
    extends Event(timestamp, source)

class StringFish extends Fish[String] {
  def onEvent(e: Event, state: String): String = {
    e match {
      case StringEvent(_, _, str) => println(s"string $str")
      case other => println(s"stringFish unrecognized message $other")
    }
    state
  }

  def onCommand(c: Command, state: String): ISeq[Event] = Vector.empty

  def initialState: String = ""
}
