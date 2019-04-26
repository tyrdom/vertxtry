package gameplayLib

case class Buff(buffEffect: BuffEffect, var lastRound: Int, var lastTurn: Int, var lastBattle: Int, var activeTimes: Int, var values: Seq[Int]) {
}

trait BuffEffect

case class PointAdd(int: Int) extends BuffEffect

case class Shield(int: Int) extends BuffEffect

case class AttackAdd(int: Int) extends BuffEffect

case class DodgeDamage(float: Float) extends BuffEffect


