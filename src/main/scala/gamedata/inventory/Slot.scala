package gamedata.inventory

final case class Slot(name : Char) {
  assert('a' <= name && name <= 'v')

  override def toString : String = name.toString
}
