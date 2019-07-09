package sample.hellopond

object Timestamp {
    var ts = 0
    def now(): Int = {
        ts += 1
        ts
    }
}