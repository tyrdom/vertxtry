package gameplayLib

import scala.collection.immutable

object Config {
  val maxPoint = 10
  val minPoint = 1
  val minStraightNeed = 5
  val maxLength: Int = maxPoint - minPoint + 1
  val minLength = 1
  val levelArray: Array[(Int, Int)] = Array(
    (0, 1),
    (2, 2),
    (5, 3),
    (9, 4),
    (15, 5),
    (24, 6),
    (36, 7),
    (52, 8),
    (72, 9),
    (97, 10)
  )

  val initHitPoint = 1000

  private val strings: immutable.IndexedSeq[String] = (1 until 10).map(x => x.toString)
  val testTuple = strings

  val testSet: Set[Int] = Seq(1, 1, 3, 4, 5).toSet

  val normalCards: Seq[Card] = (1 to 11).map(i => Card(i, i, i, false, None, Nil: Seq[Skill]))

  def genTestCharCards(cid: Int): Seq[Card] = {
    (1 to 10).map(i => Card(cid * 100 + i, i, i, false, Some(cid), Nil))
  }

  val standardCIds:Array[Int] = Array(1,2,3,4,5,6,7,8,9,10)
}
