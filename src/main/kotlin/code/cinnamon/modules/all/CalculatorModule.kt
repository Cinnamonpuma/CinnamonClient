package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.minecraft.client.MinecraftClient
import net.objecthunter.exp4j.ExpressionBuilder

class CalculatorModule : Module("Calculator", "Perform calculations in chat.") {

    private val mc = MinecraftClient.getInstance()

    override fun onEnable() {
        ClientSendMessageEvents.ALLOW_CHAT.register { message ->
            if (isMathExpression(message)) {
                val result = calculate(message)
                mc.inGameHud.chatHud.addMessage(net.minecraft.text.Text.of("[Calc] $message = $result"))
                false
            } else {
                true
            }
        }
        println("CalculatorModule enabled.")
    }

    override fun onDisable() {
        println("CalculatorModule disabled.")
    }

    private fun isMathExpression(input: String): Boolean {
        return input.matches(Regex("^[0-9+\\-*/().\\s]+$")) &&
                input.any { it in "+-*/" } &&
                input.isNotBlank()
    }

    private fun calculate(expression: String): String {
        return try {
            val result = ExpressionBuilder(expression).build().evaluate()
            result.toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
