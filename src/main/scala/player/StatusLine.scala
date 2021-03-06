package player

import gamedata.Hunger

case class StatusLine(
    level: Int,
    gold: Int,
    hp: Int,
    maxhp: Int,
    str: Int,
    maxstr: Int,
    armor: Int,
    exp: Int,
    expLevel: Int,
    hunger: Hunger
)
