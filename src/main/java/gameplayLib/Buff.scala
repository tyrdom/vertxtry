package gameplayLib

case class Buff(buffEffect: BuffEffect, var lastRound: Option[Int], var lastTurn: Option[Int], var lastBattle: Option[Int] = Some(1), var activeTimes: Option[Int]) {
}

sealed trait BuffEffect

case class PointAdd(addPoint: Int) extends BuffEffect // 计算shape时，点数会调整

case class SetPoint(point: Int) extends BuffEffect //计算shape时，点数会调整

case class Shield(ShieldPoint: Int) extends BuffEffect //计算伤害时，可以减免伤害

case class AttackAdd(addValue: Int) extends BuffEffect //计算伤害时，可以增加伤害

case class DodgeDamage(dodgeRate: Float) extends BuffEffect //计算伤害时，可以免疫一定次数的伤害

case class DefenceAdd(addValue: Int) extends BuffEffect //计算伤害时，可以减免伤害

case class CounterCardDestroyStack() extends BuffEffect //

case class DrainLife(drainPercent: Float) extends BuffEffect //计算伤害时，可以恢复攻击者生命

case class BeWatchCard(num: Int) extends BuffEffect //检查牌时，可以观察对手一定数量的牌

case class BeWatchAll() extends BuffEffect //检查牌时，可以看到对手所有牌

case class DamageBack(backPercent: Float) extends BuffEffect //计算伤害时，可以反弹攻击者伤害

case class DamageMultiply(times: Float) extends BuffEffect //计算伤害时，伤害翻倍倍数

case class DirectKillStack() extends BuffEffect // 直接击杀的buff层数