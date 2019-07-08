package gameplayLib

case class OnePlayerStatus(var defeat: Boolean = false,
                           var bombNeedNum: Int = Config.startBombNeedNum,
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
    characters.map(_.attack).sum + this.buffs.map(_.buffEffect.getAttack).sum

  }

  def getDefence: Int = {
    val characters = this.characters
    characters.map(_.defence).sum
  }

  def takeDamage(damage: Int): OnePlayerStatus
  = {
    val defenderUseShield = this.buffs.foldLeft((Nil: Seq[Buff], damage))((aTuple, aBuff) => {
      val leftDamage = aTuple._2
      val tuple: (BuffEffect, Int) = aBuff.buffEffect.useShield(leftDamage)
      val aNewBuff = aBuff.changeEffect(tuple._1)
      val newLeft = tuple._2
      val newBuffs = aTuple._1 :+ aNewBuff
      (newBuffs, newLeft)
    })
    val newBuffsAfterShieldUse = defenderUseShield._1.filterNot(_.buffEffect.emptyShield)
    val damageFromAtkAfterShield = defenderUseShield._2
    this.buffs = newBuffsAfterShieldUse

    val newHP = this.HP - damageFromAtkAfterShield
    if (newHP <= 0)
      this.defeat = true
    else
      this.defeat = false
    this
  }

  def takeHeal(heal: Int): OnePlayerStatus = {
    val newHP = this.HP + heal
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
    this.defeat = true
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

  def drawOutCardByGenId(gid: Int): (OnePlayerStatus, Option[Card]) = {
    val cards = this.handCards
    this.handCards = cards.filterNot(_.genId == gid)
    val outCard = cards.find(_.genId == gid)
    (this, outCard)
  }

  def dropACard(isMax: Boolean): (OnePlayerStatus, Card) = {
    val cards = Card.sortHandCard(this.handCards, this.characters)
    val cards2 = if (isMax) {
      cards.reverse
    } else cards
    this.handCards = Card.sortHandCard(cards2.tail, this.characters)
    val outCard = cards2.head
    (this, outCard)
  }

  def dropCards(isMax: Boolean, num: Int): (OnePlayerStatus, Seq[Card]) = {
    val cards = Card.sortHandCard(this.handCards, this.characters)
    val cards2 = if (isMax) {
      cards.reverse
    } else cards

    val tuple = cards2.splitAt(num)
    this.handCards = Card.sortHandCard(tuple._2, this.characters)
    val outCard = tuple._1
    (this, outCard)
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