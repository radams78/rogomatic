package player

import rogue.IRogue
import gamedata.inventory.item.Item
import gamedata.inventory.Inventory
import gamedata.Command

// TODO Read screen and inventory after performing command
class RoguePlayer(rogue: IRogue) {
  private val screen = rogue.getScreen()
  private val inventory = readInventoryScreen()

  def getScreen() = screen

  def getInventory() = inventory

  def performCommand(command: Command) = 
    for (k <- command.keypresses)
      rogue.sendKeypress(k)

  private def readInventoryScreen(): Inventory = {
    summonInventoryScreen()
    val inventory = Parser.parseInventoryScreen(rogue.getScreen())
    dismissInventoryScreen()
    return inventory
  }

  private def summonInventoryScreen() = rogue.sendKeypress('i')

  private def dismissInventoryScreen() = rogue.sendKeypress(' ')
}