package xyz.haloai.haloai_android_productivity.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.haloai.haloai_android_productivity.ui.viewmodel.ProductivityFeedViewModel

class SuggestedTasksWorker (
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams), KoinComponent {
    val productivityFeedViewModel: ProductivityFeedViewModel by inject()

    override fun doWork(): Result {
        // TODO: Lookup misc info db, see if suggested tasks have been generated, if not, generate.
        return runBlocking {
            try {
                // Example: miscInfoDbViewModel.generateSuggestedTasks()
                productivityFeedViewModel.updateSuggestedTasks()
                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
}