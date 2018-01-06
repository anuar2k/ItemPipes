ItemPipes
=====
**ItemPipes** is a Terasology module that adds item transport system based on pipelines. ItemPipes are useful for automatization purposes - this module lets transport items without the involvement from the player.

Items
=====

  - **Pipes** are the base of this module. They are used to transport items. You can connect them together with other pipes to make pipelines and with blocks such as Suction Blocks or chests.
  - **Suction Block** is used to input items into pipes. Suction Block attracts all items laying on the ground withing 5 blocks long radius to suck them in. If more than one pipe is connected to a Suction Block, it will put the items randomly into one of the connected pipes.
  
  _Check out **[AdditionalItemPipes](https://github.com/Terasology/AdditionalItemPipes)** for more interesting items using pipe system!_
  
Creating pipelines
=====
To create a pipeline just place two pipes next to each other. They will connect together with each other and make a pipeline. Every pipe is able to have up to 6 connections! If an item comes to a pipe which has more than two connections, it will go further into one of the other connected pipes randomly.

Integration with chests
=====
ItemPipes allows the players to connect pipelines with chests to input items into them.

![](https://user-images.githubusercontent.com/28996462/34639790-e94f8c18-f2e6-11e7-8c66-12589b3416f2.png)

_A setup like this will put every dropped item in Suction Block's proximity into the chest._
