package sample.hellopond

import scala.collection.immutable.{Seq => ISeq}

class Event(val timestamp: Int, val source: String)
class Command

trait Fish[S] {
  def onEvent(e: Event, state: S): S
  def onCommand(c: Command, state: S): ISeq[Event]
  def initialState: S
}
