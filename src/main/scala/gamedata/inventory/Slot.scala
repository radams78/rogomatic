package gamedata.inventory

case class Slot(name : Char) {
  assert('a' <= name && name <= 'v')
}
