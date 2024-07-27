package xyz.haloai.haloai_android_productivity.services.repository

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.haloai.haloai_android_productivity.services.AssistantModeFunctions
import xyz.haloai.haloai_android_productivity.services.EnumFunctionTypes
import xyz.haloai.haloai_android_productivity.ui.viewmodel.OpenAIViewModel
import xyz.haloai.haloai_android_productivity.ui.viewmodel.TextExtractionFromImageViewModel
import xyz.haloai.haloai_android_productivity.xyz.haloai.haloai_android_productivity.data.ui.screens.ChatHistory

interface AssistantModeFunctionsRepository {
    // Functions used by the Assistant Mode (AI Assistant) to perform tasks like sending emails, scheduling events, etc. for the user
    suspend fun ask_ai(conversation: List<ChatHistory.Message>): String

    suspend fun processScreenshot(screenshot: Bitmap)
}

class AssistantModeFunctionsImpl(private val assistantModeFunctions: AssistantModeFunctions): AssistantModeFunctionsRepository, KoinComponent {

    // Functions used by the Assistant Mode (AI Assistant) to perform tasks like sending emails, scheduling events, etc. for the user
    private val openAIViewModel: OpenAIViewModel by inject()
    private val textExtractionFromImageViewModel: TextExtractionFromImageViewModel by inject()

    private suspend fun ParseFunctionsListAndExecuteOps(functionsList: String): String {
        // Parse the functions list
        val parsedResponse = assistantModeFunctions.parseAssistantOutput(functionsList)

        var finalResponse = ""
        val variableMap = mutableMapOf<String, String>()

        for (instruction in parsedResponse)
        {
            val arguments: MutableList<String> = mutableListOf()
            for (args in instruction.params){
                // if argument is of format $1, $2, etc (using regex). (has to be a number, and
                // start with $)
                // val regex = Regex("\\$\\d+") // This is matching random numbers inside the string. We need to match only the numbers at the end of the string.
                val regex = Regex("^\\$\\d+$")
                if (regex.matches(args.value.trim())) {
                    arguments.add(variableMap[args.value] ?: "")
                }
                else {
                    arguments.add(args.value.trim())
                }

            }

            when (instruction.function){
                EnumFunctionTypes.CREATE_REMINDER -> {
                    assistantModeFunctions.createReminder(reminderText = arguments[0].toString(), dateAndTimeForReminder = arguments[1].toString())
                }

                EnumFunctionTypes.ADD_TO_NOTES -> {

                    assistantModeFunctions.addToNote(noteText = arguments[0].toString(), noteTitle = arguments[1].toString())
                }
                EnumFunctionTypes.RESPOND_TO_USER -> {
                    finalResponse = assistantModeFunctions.respondToUser(responseText = arguments[0].toString())
                }
                EnumFunctionTypes.CREATE_UNSCHEDULED_REMINDER -> {
                    assistantModeFunctions.createUnscheduledReminder(reminderText = arguments[0].toString())
                }
                EnumFunctionTypes.GET_NOTE_TITLES -> {
                    // add output of function to variable map
                    variableMap[instruction.varNumberToStoreIn!!] = assistantModeFunctions.getNoteTitles().toString()
                }
                EnumFunctionTypes.GET_AVAILABLE_TIMES -> {
                    variableMap[instruction.varNumberToStoreIn!!] = assistantModeFunctions.getAvailableTimes(day = arguments[0].toString(),
                        currentDateTime = arguments[1].toString()
                    )
                }
                EnumFunctionTypes.CREATE_EVENT -> {
                    assistantModeFunctions.createCalendarEvent(
                        text = arguments[0].toString(),
                        description = arguments[1].toString(),
                        startDateTime = arguments[2].toString(),
                        endDateTime = arguments[3].toString()
                    )
                }
                EnumFunctionTypes.SUMMARIZE_TEXT -> {
                    variableMap[instruction.varNumberToStoreIn!!] = assistantModeFunctions.summarizeText(prompt = arguments[0].toString())
                }
                EnumFunctionTypes.ASK_AI -> {
                    variableMap[instruction.varNumberToStoreIn!!] = assistantModeFunctions.askAi(prompt = arguments[0].toString())
                }
                EnumFunctionTypes.GET_CONTACT -> {
                    variableMap[instruction.varNumberToStoreIn!!] = assistantModeFunctions.getContact(name = arguments[0].toString())
                }
                EnumFunctionTypes.SAVE_CONTACT -> {
                    assistantModeFunctions.saveContact(
                        name = arguments[0].toString(),
                        email = arguments[1].toString(),
                        phone = arguments[2].toString()
                    )
                }
                EnumFunctionTypes.CREATE_LT_GOAL -> {
                    assistantModeFunctions.createLtGoal(
                        goalText = arguments[0].toString(),
                        deadline = arguments[1].toString()
                    )
                }
            }
        }
        return finalResponse
    }

    // Business Logic
    override suspend fun ask_ai(conversation: List<ChatHistory.Message>): String {
        // Ask the AI a question
//        val rawResponse = assistantModeFunctions.ask_ai(conversation)
        val functionsList = assistantModeFunctions.convert_chat_to_functions(conversation)
        val response = ParseFunctionsListAndExecuteOps(functionsList)
        return response
    }

    override suspend fun processScreenshot(screenshot: Bitmap) {
        // Process the screenshot
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val text = textExtractionFromImageViewModel.getTextFromBitmap(screenshot!!, coroutineScope)
        val functionsList = assistantModeFunctions.convertScreenshotTextToFunctions(text)
        ParseFunctionsListAndExecuteOps(functionsList)
    }

}