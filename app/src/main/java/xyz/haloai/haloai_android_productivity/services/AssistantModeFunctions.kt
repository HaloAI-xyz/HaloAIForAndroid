package xyz.haloai.haloai_android_productivity.services

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import xyz.haloai.haloai_android_productivity.data.local.entities.enumFeedCardType
import xyz.haloai.haloai_android_productivity.ui.viewmodel.NotesDbViewModel
import xyz.haloai.haloai_android_productivity.ui.viewmodel.OpenAIViewModel
import xyz.haloai.haloai_android_productivity.ui.viewmodel.ProductivityFeedViewModel
import xyz.haloai.haloai_android_productivity.ui.viewmodel.ScheduleDbViewModel
import xyz.haloai.haloai_android_productivity.xyz.haloai.haloai_android_productivity.data.ui.screens.ChatHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class AssistantModeFunctions(private val context: Context): KoinComponent {
    // Functions used by the Assistant Mode (AI Assistant) to perform tasks like sending emails, scheduling events, etc. for the user
    val openAIViewModel: OpenAIViewModel by inject()
    val scheduleDbViewModel: ScheduleDbViewModel by inject { parametersOf(context) }
    val notesDbViewModel: NotesDbViewModel by inject()
    val productivityFeedViewModel: ProductivityFeedViewModel by inject()

    //    suspend fun ask_ai(question: List<ChatHistory.Message>): String {
//        // Ask the AI a question
//        val intermediateResponse = convert_chat_to_functions(conversation=question)
//        val parsedResponse = parseAssistantOutput(intermediateResponse)
//
//        return openAIViewModel.getChatGPTResponse("I am your AI Assistant. How can I help you?", question)
//    }
    fun getCurrentTimeFormatted(): String {
        val now = Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault())
        return formatter.format(now)
    }

    suspend fun convert_chat_to_functions(conversation: List<ChatHistory.Message>): String {
        val currentDateTime = getCurrentTimeFormatted()
        val output = openAIViewModel.getChatGPTResponse(
            "You are a helpful personal assistant. You are a productivity focused assistant, with access to the following functions:  \n" +
                    "1. create_reminder(string reminderText, string dateAndTimeForReminder): This will create a reminder with the given text at the given time. The datetime should be in \"YYYY-MM-DD HH:MM:SS\" format.[Returns nothing] \n" +
                    "2. create_unscheduled_reminder(string reminderText): This will create an unscheduled reminder.[Returns nothing] \n" +
                    "3. respond_to_user(string responseText): This will send the response back to the user. Use it to ask for details or tell the user what you did. Keep it brief and concise. [Returns string output] \n" +
//                    "4. get_note_titles(): Gets a note title. [Will return a string value]  \n" +
                    "5. add_to_note(string noteText, string noteTitle): Adds the noteText to the note with title noteTitle. [Returns nothing] \n" +
                    "6. ask_ai(string prompt): Use this to ask an AI Agent to perform a task. [Returns a string output]  \n" +
                    "7. summarize_text(string inputText): Give a summary of the text which can be used for notes or general description of tasks and events. [ returns a string value] \n" +
                    "8. create_lt_goal(string goalText, string deadline): Returns nothing.\n" +
                    "This will create a long term goal (things like studying for giving the GRE, " +
                    "learning a new instrument, planning to study for an exam, etc. are long-term " +
                    "goals). The deadline is defaulted " +
                    "to one year from now if no other information is presented." +
                    " \n" +
                    "The datetime should be a string in \"YYYY-MM-DD HH:MM:SS\" format, and contain nothing else." +
                    //"8. get_contact(string name): Search for contact details and get a number " + "and email. \n" +
                    "9. create_calendar_event(string text, string description, string startDateTime, string endDateTime): This will add a calendar event to the users calendar using the details provided. The datetime should be in \"YYYY-MM-DD HH:MM:SS\" format.\n" +
                    //"10. save_contact(string name, string email, string number): This stores
                    // the contacts details mentioned in user's device. \n" +
                    //"11. get_available_times(string day, string currentDateTime): This gives a " +
                    //"list of available times that the user is free on the specific day
                    // mentioned [returns an object with startDatetime and endDatetime]. The datetime should be in \"YYYY-MM-DD HH:MM:SS\" format. \n" +
                    "Your job is to output a series of function calls from the functions given above to help the user complete their task. \n" +
                    "Remember, you can only help with productivity related tasks. \n" +
                    "If the user does not give any indication of what they wanted you to do, you can assume they wanted to add it to their notes. \n" +
                    "The current time is " + currentDateTime + ", you can use this time as reference where you have to calculate the time required and make sure to keep all the datetime format the same.\n" +
                    "Use your best judgement to decide if adding a reminder or adding to notes would be better. Don't produce any comments in output. \n" +
                    "If any small calculations are required you can use ask_ai function to make a call to complete the task. \n" +
                    "Don't use any calculations as functions parameter in function call.If the user input implies an action that is not time-specific, create an unscheduled reminder. For example: \n" +
                    "User Input: Have to check tasks created from today’s transcripts \n" +
                    "Your Output: \n" +
                    "\$1 = create_unscheduled_reminder(“Check tasks created from today’s transcripts”) \n" +
                    "respond_to_user(“Got it, I’ve added an unscheduled reminder to check tasks created from today’s transcripts.”) \n" +
                    "Examples: User Input: Remember to submit the project proposal for the software engineering class by Friday.  \n" +
                    "Your Output: \$1 = get_available_times(string \"Friday\", <current datetime> ) create_reminder(“Submit Project Proposal for software engineering class”, “\$1”) respond_to_user(“Got it, I’ve added a reminder.”)  \n" +
                    "User Input: Aws login > S3 > Connect IAM Role \n" +
                    " Your Output: \$1 = get_note_titles()  \n" +
                    "\$2 = ask_ai(“Here is a piece of text the user wants to add to their notes: \$prompt. From the following notes, give me the name of the note this text would belong to: \$1. To indicate creation of a new note, respond with the name of the suggested new note. Only create a note when absolutely needed.”)  \n" +
                    "add_to_note(“\$userInput”, \$2)  \n" +
                    "respond_to_user(“I’m adding this to \$2”) \n" +
                    "Note, you can refer to the output of certain functions by using the \$<varNum> notation, which will be an int like \$1, \$2 and so on and the original user input using \$userInput. \n" +
                    "Ensure you provide a response to the user, and tell them what you are doing. Keep it brief and concise.\n"
                    ,
            conversation.last().toString(),
            modelToUse = "gpt-4o",
            temperature = 0.0
        )
        // TODO: Add entire conversation to prompt
        if (output != null) {
            return output

        }
        return null.toString()
    }

    suspend fun convertScreenshotTextToFunctions(text: String): String {
        val currentDateTime = getCurrentTimeFormatted()
        val initialPromptText = "You are a helpful personal assistant. You are a productivity focused assistant, with access to the following functions:  \n" +
                "1. create_reminder(string reminderText, string dateAndTimeForReminder): Returns nothing.\n" +
                "This will create a reminder with the given text at the given time. The datetime " +
                "should be a string in \"YYYY-MM-DD HH:MM:SS\" format, and contain nothing else. " +
                "\n" +
                "2. create_unscheduled_reminder(string reminderText): Returns nothing.\n" +
                "This will create an unscheduled reminder. \n" +
                "3. get_note_titles(): Returns string.\n" +
                "Gets the titles of all notes on device (separated by \"<DELIM>\")\n" +
                "4. add_to_note(string noteText, string noteTitle): Returns nothing.\n" +
                "Adds the noteText to the note with title noteTitle.  \n" +
                "5. ask_ai(string prompt): Returns string.\n" +
                "Use this to ask an AI Agent to perform a task.\n" +
                "6. summarize_text(): Returns string.\n" +
                "Give a summary of the text which can be used for notes or general description of" +
                " tasks and events.\n" +
                // "7. get_contact(string name): Returns string.\n" +
                // "Search for contact details and get a number and email. (separated by " +
                // "\"<DELIM>\")\n" +
                "7. create_calendar_event(string text, string description, string startDateTime, " +
                "string endDateTime): Returns nothing.\n" +
                "This will add a calendar event to the users calendar using the details provided." +
                " The datetimes should be a string in \"YYYY-MM-DD HH:MM:SS\" format, and contain nothing else." +
                "8. create_lt_goal(string goalText, string deadline): Returns nothing.\n" +
                "This will create a long term goal (things like studying for giving the GRE, " +
                "learning a new instrument, planning to study for an exam, etc. are long-term " +
                "goals). The deadline is defaulted " +
                "to one year from now if no other information is present." +
                " \n" +
                "The datetime should be a string in \"YYYY-MM-DD HH:MM:SS\" format, and contain nothing else." +
                // "9. save_contact(string name, string email, string number): Returns nothing.\n" +
                // "This stores the contacts details mentioned in user's device. \n" +
                // "10. get_available_times(string day, string currentDateTime): Returns string
                // .\n" +
                // "This gives a list of available times that the user is free on the specific day
                // " +
                // "mentioned [returns an object with startDatetime and endDatetime]. The
                // datetimes \" +\n" +
                // "                \"should be a string in \"YYYY-MM-DD HH:MM:SS\" format, and " +
                //"contain nothing else. Returns a blob about when the user is free (will need to
                // be parsed by ai).\n" +
                "\n" +
                "INSTRUCTIONS:\n" +
                "- You will be given text extracted from a screenshot the user has taken of their phone, and output a series of function calls from the functions given above to help the user complete their task. You can perform multiple tasks.\n" +
                "- Remember, you can only help with productivity related tasks. \n" +
                "- If it is generic information on the screen that can't be converted to an event, task, reminder or contact, you can assume they wanted to add it to their notes. \n" +
                "- The current time is +" +
                currentDateTime +
                ", you can use this time as reference where you have to calculate the time required and make sure to keep all the datetime format the same.\n" +
                "- Use your best judgement to decide if adding a reminder or adding to notes would be better. \n" +
                "- Don't produce any comments in output. \n" +
                "- Don't create a note if you are creating events/tasks/reminders/etc.\n" +
                "- If any small calculations are required you can use ask_ai function to make a call to complete the task. \n" +
                "- If there is text like \"tomorrow\", \"day after\", convert it to the exact date.\n" +
                "- Don't use any calculations as functions parameter in function call.\n" +
                "- If the user input implies an action that is not time-specific, create an unscheduled reminder.  \n" +
                "- Note, you can refer to the output of certain functions by using the \$<varNum> notation, which will be an int like \$1, \$2 and so on and the original user input using \$userInput. (See example below)\n" +
                "\n" +
                "Here is an example:\n" +
                "\n" +
                "Input: \n" +
                "10:26 0;\n" +
                "All\n" +
                "Assignments\n" +
                "Homework #1\n" +
                "This is a group assignment for practice. By\n" +
                "'practice, I mean that the assignment will be\n" +
                "collected and graded on a 10-point scale: 10 p...\n" +
                "Due: Jul 17, 2024 9:00 AM\n" +
                "Qill Y Rl 49%\n" +
                "Pre-class 5: Stealing in baseball\n" +
                "You are welcome to discuss this question in a\n" +
                "group, but your answer should be submitted\n" +
                "individually.\n" +
                "Due: Jul 17, 2024 9:00 AM\n" +
                "Pre-class 7: Decision analysis and hockey\n" +
                "You are welcome to discuss this question in a\n" +
                "group, but your answer should be submitted\n" +
                "individually.\n" +
                "Due: Jul 18, 2024 9:00 AM\n" +
                "Homework #2\n" +
                "Due: Jul 18, 2024 9:00 AM\n" +
                "nrobahilitiec\n" +
                "Pre-class 8: Computing win, tie and loss " +
                "\n" +
                "Your Output: \n" +
                "create_reminder(\"Homework #1\", \"2024-07-17 09:00:00\")\n" +
                "create_reminder(\"Pre-class 7: Decision analysis and hockey\", \"2024-07-18 09:00:00\")\n" +
                "create_reminder(\"Homework #2\", \"2024-07-18 09:00:00\")\n" +
                "\n"

        val output = openAIViewModel.getChatGPTResponse(
            initialPromptText,
            text,
            modelToUse = "gpt-4o"
        )
        // TODO: Add entire conversation to prompt
        if (output != null) {
            return output
        }
        return null.toString()
    }

    fun createReminder(reminderText: String, dateAndTimeForReminder: String): String {
        CoroutineScope(Dispatchers.IO).launch {
            // The date is in "YYYY-MM-DD HH:MM:SS" format
            val deadlineAsDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateAndTimeForReminder)
            productivityFeedViewModel.insertFeedCard(
                title = reminderText,
                description = "Reminder",
                extraDescription = "At $dateAndTimeForReminder",
                deadline = deadlineAsDate,
                importanceScore = 4,
                primaryActionType = enumFeedCardType.POTENTIAL_TASK
            )
        }

        return "Reminder created for '$reminderText' at $dateAndTimeForReminder"
    }

    fun createUnscheduledReminder(reminderText: String): String {
        CoroutineScope(Dispatchers.IO).launch {
            productivityFeedViewModel.insertFeedCard(
                title = reminderText,
                description = "Unscheduled Reminder",
                importanceScore = 4,
                primaryActionType = enumFeedCardType.POTENTIAL_TASK
            )
        }
        return "Unscheduled reminder created for '$reminderText'"
    }

    fun respondToUser(responseText: String): String {
        return responseText
    }

    suspend fun getNoteTitles(): List<String> {
        val allNoteTitles = withContext(Dispatchers.IO) {
            return@withContext notesDbViewModel.getAllNotes().map { it.title }
        }
        // val allNoteTitles = notesDbViewModel.getAllNotes().map { it.title }
        return allNoteTitles
        // return listOf("Meeting Notes", "Project Ideas", "Todo List")
    }

    fun addToNote(noteText: String, noteTitle: String): String {
        CoroutineScope(Dispatchers.IO).launch {
            productivityFeedViewModel.insertFeedCard(
                title = noteTitle,
                description = noteText,
                importanceScore = 4,
                primaryActionType = enumFeedCardType.POTENTIAL_NOTE
            )
        }
        return "Added to '$noteTitle': $noteText"
    }

    suspend fun askAi(prompt: String): String {
        return openAIViewModel.getChatGPTResponse(
            "INSTRUCTIONS: You will be given the task to perform. Only respond with the exact required output. Don't use any additional formatting.",
            prompt,modelToUse = "gpt-4o-mini"
        )
    }

    fun summarizeText(prompt:String): String {
        return "Summary of the text"
    }

    fun getContact(name: String): String {
        return "123-456-7890"
    }

    fun createCalendarEvent(
        text: String,
        description: String,
        startDateTime: String,
        endDateTime: String
    ): String {
        CoroutineScope(Dispatchers.IO).launch {
            productivityFeedViewModel.insertFeedCard(
                title = text,
                description = description,
                extraDescription = "From $startDateTime to $endDateTime",
                importanceScore = 4,
                primaryActionType = enumFeedCardType.POTENTIAL_EVENT
            )
        }
        return "Calendar event '$text' created with description '$description' from $startDateTime to $endDateTime"
    }

    fun saveContact(name: String, email: String, phone: String): String {
        return "Contact saved: $name, Email: $email, Phone: $phone"
    }

    fun createLtGoal(goalText: String, deadline: String): String {
        CoroutineScope(Dispatchers.IO).launch {
            // The date is in "YYYY-MM-DD HH:MM:SS" format
            val deadlineAsDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(deadline)
            productivityFeedViewModel.insertFeedCard(
                title = goalText,
                description = "Long Term Goal",
                extraDescription = "Deadline: $deadline",
                deadline = deadlineAsDate,
                importanceScore = 4,
                primaryActionType = enumFeedCardType.POTENTIAL_LTGOAL
            )
        }

        return "Long term goal created for '$goalText' with deadline $deadline"
    }

    fun getAvailableTimes(day: String, currentDateTime: String): String {
        // TODO: Fetch available times from the schedule database after asking for an update
        val date =
            SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()).parse(currentDateTime)
        val formattedDate =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
        return "Available times on $day: $formattedDate"
    }

    fun getFunctionType(input: String): EnumFunctionTypes {
        return when (input) {
            "get_available_times" -> EnumFunctionTypes.GET_AVAILABLE_TIMES
            "create_reminder" -> EnumFunctionTypes.CREATE_REMINDER
            "create_unscheduled_reminder" -> EnumFunctionTypes.CREATE_UNSCHEDULED_REMINDER
            "get_note_titles" -> EnumFunctionTypes.GET_NOTE_TITLES
            "add_to_note" -> EnumFunctionTypes.ADD_TO_NOTES
            "respond_to_user" -> EnumFunctionTypes.RESPOND_TO_USER
            "create_calendar_event" -> EnumFunctionTypes.CREATE_EVENT
            "summarize_text" -> EnumFunctionTypes.SUMMARIZE_TEXT
            "ask_ai" -> EnumFunctionTypes.ASK_AI
            "get_contact" -> EnumFunctionTypes.GET_CONTACT
            "save_contact" -> EnumFunctionTypes.SAVE_CONTACT
            "create_lt_goal" -> EnumFunctionTypes.CREATE_LT_GOAL
            // "convert_chat_to_function" -> EnumFunctionTypes.CONVERT_CHAT_TO_FUNCTION
            else -> throw IllegalArgumentException("Unknown function name: $input")
        }

    }

    suspend fun parseAssistantOutput(inputText: String): List<FunctionsWithParams> {
        val tasks = mutableListOf<FunctionsWithParams>()
        val lines = inputText.trim().split("\n")

        val functionPattern = Pattern.compile("\\$(\\d+)\\s*=\\s*(\\w+)\\((.*)\\)")
        val responsePattern = Pattern.compile("\\s*(\\w+)\\((.*)\\)")

        for (line in lines) {

            var matcher_1 = functionPattern.matcher(line)
            var matcher_2 = responsePattern.matcher(line)
            if (matcher_1.find()) {
                val varNumber = matcher_1.group(1)
                val funcName = matcher_1.group(2)
                val args = matcher_1.group(3)

//                val argsList = args.split(",").map { it.trim().trim('"') }
                val argsList = if (funcName == "respond_to_user") {
                    listOf(args.trim().trim('"'))
                } else {
                    args.split(",").map { it.trim().trim('"') }
                }
                val paramsMap = argsList.mapIndexed { index, arg -> "arg$index" to arg }.toMap()

                val functionType = getFunctionType(input = funcName)

                tasks.add(
                    FunctionsWithParams(
                        function = functionType,
                        params = paramsMap,
                        varNumberToStoreIn = "\$$varNumber"
                    )
                )
            } else if (matcher_2.find()) {
//                val varNumber = matcher_2.group(1)
                val funcName = matcher_2.group(1)
                val args = matcher_2.group(2)

                // val argsList = args.split(",").map { it.trim().trim('"') }
                val pattern = Regex("""\$\d+|".*?"""") // Matches $1, $2, $3, etc. and "string"
//                val argsList = pattern.findAll(args?.toString() ?: "").map { match ->
//                    match.value.trim('"')
//                }.toList()
                val argsList = if (funcName == "respond_to_user") {
                    listOf(args.trim().trim('"'))
                } else {
                    val pattern = Regex("""\$\d+|".*?"""") // Matches $1, $2, $3, etc. and "string"
                    pattern.findAll(args?.toString() ?: "").map { match ->
                        match.value.trim('"')
                    }.toList()
                }
                val paramsMap = argsList.mapIndexed { index, arg -> "arg$index" to arg }.toMap()

                val functionType = getFunctionType(input = funcName)

                tasks.add(
                    FunctionsWithParams(
                        function = functionType,
                        params = paramsMap,
                        varNumberToStoreIn = null
                    )
                )
            }
        }
        return tasks
    }
}

data class FunctionsWithParams(
    val function: EnumFunctionTypes,
    val params: Map<String, String>,
    val varNumberToStoreIn: String? = null,
)

enum class EnumFunctionTypes {
//     CONVERT_CHAT_TO_FUNCTION,
    ADD_TO_NOTES,
//    SEND_EMAIL,
    RESPOND_TO_USER,
    CREATE_REMINDER,
    CREATE_UNSCHEDULED_REMINDER,
    GET_NOTE_TITLES,
    GET_AVAILABLE_TIMES,
    CREATE_EVENT,
    SUMMARIZE_TEXT,
    ASK_AI,
    GET_CONTACT,
    SAVE_CONTACT,
    CREATE_LT_GOAL
}