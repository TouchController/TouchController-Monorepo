package top.fifthlight.armorstand.state

object NetworkModelSyncer {
    /*private val packetSender = MutableStateFlow<PacketSender?>(null)*/

    fun initialize() {
        /*ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            packetSender.value = sender
        }
        ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
            packetSender.value = null
        }
        ArmorStand.instance.scope.launch {
            packetSender.collectLatest { sender ->
                var hasSetModel = false
                ConfigHolder.config
                    .map { Pair(it.modelPath, it.sendModelData) }
                    .distinctUntilChanged()
                    .collect { (modelPath, sendModelData) ->
                        if (sendModelData) {
                            val hash = modelPath?.let { ModelManagerHolder.instance.getModelByPath(it)?.hash }
                            sender?.sendPacket(ModelUpdateC2SPayload(hash))
                            hasSetModel = true
                        } else if (hasSetModel) {
                            sender?.sendPacket(ModelUpdateC2SPayload(null))
                            hasSetModel = false
                        }
                    }
            }
        }*/
    }
}
