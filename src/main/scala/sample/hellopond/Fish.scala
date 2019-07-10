package sample.hellopond

import scala.collection.immutable.{Seq => ISeq}

// Roots of class hierarchies for Events and Commands - inherit your own from them
class Event(val timestamp: Int, val source: String)
class Command

// The Fish interface
trait Fish[S] {
  def onEvent(e: Event, state: S): S
  def onCommand(c: Command, state: S): ISeq[Event]
  def initialState: S
}
