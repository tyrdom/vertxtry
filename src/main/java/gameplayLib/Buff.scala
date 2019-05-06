package gameplayLib

case class Buff(buffEffect: BuffEffect, var lastRound: Option[Int], var lastTurn: Option[Int], var lastBattle: Option[Int] = Some(1), var activeTimes: Option[Int]) {
}

sealed trait BuffEffect

case class PointAdd(addPoint: Int) extends BuffEffect

case class SetPoint(point: Int) extends BuffEffect

case class Shield(ShieldPoint: Int) extends BuffEffect

case class AttackAdd(addValue: Int) extends BuffEffect

case class DodgeDamage(dodgeRate: Float) extends BuffEffect

case class DefenceAdd(addValue: Int) extends BuffEffect

case class CounterCardDestroyStack() extends BuffEffect

case class DrainLife(drainPercent: Float) extends BuffEffect

case class BeWatchCard(num: Int) extends BuffEffect

case class BeWatchAll() extends BuffEffect

case class DamageBack(backPercent: Float) extends BuffEffect

case class GoVictory(needNum: Int) extends BuffEffect

case class DamageMultiply(times: Float) extends BuffEffect

case class BuffTrick(Cond: Int, buffEffect: BuffEffect) extends BuffEffect

case class DirectKillStack() extends BuffEffect