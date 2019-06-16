package gameplayLib

case class OnePlayerStatus(var bombNeedNum: Int = Config.startBombNeedNum,
                           var handCards: Seq[Card] = Nil: Seq[Card], //handCards ：手上的牌
                           var HP: Int = Config.initHitPoint, //最初的属性
                           var buffs: Seq[Buff] = Nil: Seq[Buff],
                           var characters: Seq[Character] = Nil: Seq[Character],
                           var needCounter: NeedCounter = NeedCounter.EmptyNeedCounter) //需要对抗的牌型和历史记录,如果为空则不需要按照对抗出牌
{
  def addCharacters(cSeq: Seq[Character]): OnePlayerStatus = {
    this.characters = cSeq ++ this.characters
    this
  }

  def drawAPlayerCards(Cards: Seq[Card]): OnePlayerStatus = {
    this.handCards = Card.sortHandCard(Cards ++ this.handCards, characters)
    this
  }

  def addBoomNum: OnePlayerStatus = {
    val newNum = this.bombNeedNum + 1
    this.bombNeedNum = newNum
    this
  }

  def spendCards(Idx: Array[Int]): OnePlayerStatus = {
    val hd = this.handCards
    this.handCards = ((1 to hd.count(_ => true)).toSet -- Idx.toSet).map(i => hd(i - 1)).toSeq
    this.characters.foreach(x => x.addExp(Idx.length))
    this
  }

  def clearNeedCounter(): OnePlayerStatus = {
    this.needCounter = new NeedCounter(None, Nil)
    this
  }

  def putNeedCounter(newNeedCounter: NeedCounter): OnePlayerStatus = {
    this.needCounter = newNeedCounter
    this
  }

  def getAtk: Int = {
    val characters = this.characters
    characters.foldLeft(0)((sum, character) => sum + character.attack)
  }

  def getDefence: Int = {
    val characters = this.characters
    characters.foldLeft(0)((sum, character) => sum + character.defence)
  }

  def takeHPDamage(DamageSeq: Seq[Int]): OnePlayerStatus
  = {
    val newHP = this.HP - DamageSeq.sum
    this.HP = newHP
    this
  }

  def addBuff(buff: Buff): OnePlayerStatus = {
    val newBuffSeq = buff +: this.buffs
    this.buffs = newBuffSeq
    this
  }

  def kill: OnePlayerStatus = {
    this.HP = -999
    this
  }

  def addHandCardBuff(buff: Buff): OnePlayerStatus = {
    this.handCards = this.handCards.map(_.addBuff(buff))
    this
  }

  def delHandCardBuff(buffEffect: BuffEffect): OnePlayerStatus = {
    this.handCards = this.handCards.map(_.delBuff(buffEffect))
    this
  }

  def delBuffFromPlayer(buffEffect: BuffEffect): OnePlayerStatus = {
    this.buffs = this.buffs.filter(b => b.buffEffect != buffEffect)
    this
  }

  def addHandCard(cards: Seq[Card]): OnePlayerStatus = {
    this.handCards = Card.sortHandCard(this.handCards ++ cards, this.characters)
    this
  }
}

case class SpawnedCard(who: String, cards: Seq[Card])

case class NeedCounter(shape: Option[Shape],
                       counterHistorySpawn: Seq[SpawnedCard])

object NeedCounter {
  def EmptyNeedCounter: NeedCounter = NeedCounter(None, Nil)
}

//needCounterShape：需要对抗的牌型，如果有出牌权，那么就需要对抗此Shape
//counterHistorySpawn：对抗的历史出牌，如果有需要对抗时对抗失败，则消灭这个needCounter，触发一些效果，对抗成功则把needCounter加入自己出的牌shape的点数，更新shape再转移给其他玩家