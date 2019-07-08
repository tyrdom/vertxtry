package gameplayLib

case class Buff(buffEffect: BuffEffect,
                var lastRound: Option[Int],
                var lastTurn: Option[Int],
                var lastBattle: Option[Int] = Some(1)
               ) {
  def changeEffect(buffEffect: BuffEffect): Buff = {
    Buff(buffEffect,
      this.lastRound, this.lastTurn, this.lastBattle)
  }

}

sealed trait BuffEffect {

  def beSilence: Boolean = false

  def setPoint(point: Int): Int = point

  def addPoint(point: Int): Int = point

  def getAttack: Int = 0

  def getDefence: Int = 0

  def getAtkMultiply: Float = 1

  def getDefMultiply: Float = 1

  def getDodgeStack: Int = 0

  def useShield(damage: Int): (BuffEffect, Int) = (this, damage)

  def backDamageMulti: Float = 0f

  def drainLifeMulti: Float = 0f

  def emptyShield: Boolean = false
}

case class silence() extends BuffEffect {
  override def beSilence: Boolean = true
}

case class PointAdd(addPoint: Int) extends BuffEffect // 计算shape时，点数会调整
{
  override def addPoint(point: Int): Int = point + addPoint
}

case class SetPoint(setPoint: Int) extends BuffEffect //计算shape时，点数会调整
{
  override def setPoint(point: Int): Int = setPoint
}

case class Shield(shieldPoint: Int) extends BuffEffect //计算伤害时，可以减免伤害
{
  override def useShield(damage: Int): (BuffEffect, Int) = {
    if (shieldPoint > damage) {
      (Shield(shieldPoint - damage), 0)
    }
    else (Shield(0), damage - shieldPoint)
  }

  override def emptyShield: Boolean = shieldPoint <= 0

}

case class AttackAdd(addValue: Int) extends BuffEffect //计算伤害时，可以增加伤害
{
  override def getAttack: Int = addValue
}

case class DodgeDamage(stack: Int) extends BuffEffect //计算伤害时，可以免疫一定次数的伤害
{
  override def getDodgeStack: Int = stack
}

case class damageMultiForAtk(multi: Float) extends BuffEffect {
  override def getAtkMultiply: Float = multi match {
    case m if m <= 0 => 1
    case _ => multi
  }
}

case class damageMultiForDef(multi: Float) extends BuffEffect {
  override def getDefMultiply: Float = multi match {
    case m if m <= 0 => 1
    case _ => multi
  }
}

case class DefenceAdd(addValue: Int) extends BuffEffect //计算伤害时，可以减免伤害
{
  override def getDefence: Int = addValue
}

case class CounterCardDestroyStack() extends BuffEffect //

case class DrainLife(drainPercent: Float) extends BuffEffect //计算伤害时，可以恢复攻击者生命
{
  override def drainLifeMulti: Float = drainPercent
}

case class BeWatchCard(num: Int) extends BuffEffect //检查牌时，会被持有看牌buff的玩家看到一定数量的牌

case class WatchCard() extends BuffEffect //检查牌时，会看到有被看牌buff的玩家的牌

case class BeWatchAll() extends BuffEffect //检查牌时，会被持有看牌buff的玩家看到所有牌

case class DamageBack(backPercent: Float) extends BuffEffect //计算伤害时，可以反弹攻击者伤害
{
  override def backDamageMulti: Float = backPercent
}

case class DirectKillStack() extends BuffEffect // 直接击杀的buff层数