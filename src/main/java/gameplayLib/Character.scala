package gameplayLib

case class Character(id: Int, name: String, var level: Int, var atk: Int, var defence: Int, var exp: Int) //也是有状态的
{
  def addExp(amount: Int): Unit = {
    this.exp = exp + amount
    this.level = foundLevel(exp)
  } //必须用此方法增加经验，才能触发级别改变

  def foundLevel(exp: Int): Int = Config.levelArray.filter(x => x._1 <= exp).maxBy(_._2)._2


}

