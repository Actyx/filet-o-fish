package sample.hellopond

// broken Timestamp generator (but useful enough)
object Timestamp {
  var ts = 0
  def now(): Int = {
    ts += 1
    ts
  }
}
