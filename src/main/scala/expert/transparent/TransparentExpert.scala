package expert.transparent

import gamedata.inventory.Inventory
import gamedata.Command
import expert.IExpert

private[transparent] class TransparentExpert(user : IUser) extends IExpert {
  override def getCommand(screen : Seq[String], inventory : Inventory) : Command = {
      user.displayScreen(screen)
      user.displayInventory(inventory)
      return user.getCommand()
  }
}
