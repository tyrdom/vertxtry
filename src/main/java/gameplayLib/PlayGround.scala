package gameplayLib

case class State(var drawDeck: Seq[Card], var dropDeck: Seq[Card], var playersHand: Map[Int, Seq[Card]], var nowShape: Shape, //
                 characterPool: Seq[Character], var turn: Int, var round: Int,
                 var spawnRight: Int, var playerStatus: Map[Int, PlayerStatus], order: Seq[Int]) //可变状态

case class PlayerStatus(var hp: Int, var atk: Int, var defence: Int, var buffs: Seq[Buff], var characters: Seq[Character]) //除了位置，其他均可变

class PlayGround(state: State) { //每个房间需new1个新的playground

  def initPlayGround(playerNum: Int,characters: Seq[Character]): Unit = ??? //初始化玩家状态

  def

}
