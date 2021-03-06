package gameplayLib

case class Character(id: Int, var level: Int, var attack: Int, var defence: Int, var exp: Int) {
  def addExp(amount: Int): Unit = { //必须用此方法增加经验，才能触发级别改变
    this.exp = exp + amount
    val beforeLevel = this.level
    val afterLevel = foundLevel(exp)
    this.level = afterLevel
    if (afterLevel != beforeLevel) {
      val attrSeq = Config.attrMap.get(id)
      if (attrSeq.isEmpty) {
        this.attack = 0
        this.defence = 0
      }
      else {
        this.attack = Config.attrMap(id)._1(afterLevel - 1)

        this.defence = Config.attrMap(id)._2(afterLevel - 1)
      }
    }
  }

  def foundLevel(exp: Int): Int = Config.levelArray.filter(x => x._1 <= exp).maxBy(_._2)._2

} //有状态的数据，这个
object Character {

  def initCharacter(id: Int): Character = Character(id = id, level = 1, attack = 0, defence = 0, exp = 0) //TODO 根据数据查找1级的属性

}

