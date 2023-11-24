package me.matteraga.appvariazioniscolastiche.workers

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import me.matteraga.appvariazioniscolastiche.R
import me.matteraga.appvariazioniscolastiche.utilities.NotificationUtils
import me.matteraga.appvariazioniscolastiche.utilities.StorageUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

class CheckChangesWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    private val changesUrl =
        "https://www.ispascalcomandini.it/variazioni-orario-istituto-tecnico-tecnologico/"
    private val client = OkHttpClient()

    private lateinit var date: LocalDate //LocalDate.now().plusDays(1L)
    private lateinit var fileName: String

    private val storageUtils = StorageUtils(context)
    private val notificationUtils = NotificationUtils(context)

    private val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    private val schoolClass = sharedPref.getString("schoolClass", "1A") ?: "1A"

    // Mostra notifica
    private suspend fun startForegroundService() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                NotificationCompat.Builder(context, "changes")
                    .setSmallIcon(R.drawable.ic_search)
                    .setContentTitle("Controllo variazioni in corso...")
                    .setProgress(0, 0, true)
                    .build()
            )
        )
    }

    // Richiesta HTTP
    private fun makeRequest(url: String): Response {
        return client.newCall(
            Request.Builder().apply {
                url(url)
            }.build()
        ).execute()
    }

    override suspend fun doWork(): Result {
        try {
            startForegroundService()

            // Prende la data da controllare dai parametri del worker
            date = LocalDate.parse(inputData.getString("date") ?: return Result.failure())
            fileName = "Variazioni-${date}.pdf"

            // Sito scuola
            val changesUrlResponse = makeRequest(changesUrl)
            if (!changesUrlResponse.isSuccessful) {
                changesUrlResponse.close()
                return Result.failure()
            }

            val html = changesUrlResponse.body?.string() ?: ""
            changesUrlResponse.close()

            // Trova il link al pdf
            val pdfs = Jsoup.parse(html).select("a[href$=.pdf]").toList()
            val pdfUrl = pdfs.find { pdf ->
                pdf.attr("href").contains(
                    date.format(
                        DateTimeFormatter.ofPattern("dd-MMMM-yyyy", Locale.ITALIAN)
                    ), true
                ) || pdf.attr("href").contains(
                    date.format(
                        DateTimeFormatter.ofPattern("dd-MMMM-yy", Locale.ITALIAN)
                    ), true
                ) || pdf.attr("href").contains(
                    date.format(
                        DateTimeFormatter.ofPattern("d-MMMM", Locale.ITALIAN)
                    ), true
                )
            }?.attr("href")

            if (pdfUrl == null) {
                notificationUtils.sendBrowserNotification(
                    changesUrl,
                    "Variazioni",
                    "PDF variazioni non trovato."
                )
                return Result.failure()
            }

            // Scarica il pdf
            val pdfUrlResponse = makeRequest(pdfUrl)
            if (!pdfUrlResponse.isSuccessful) {
                pdfUrlResponse.close()
                return Result.failure()
            }

            val bytes = pdfUrlResponse.body?.bytes() ?: byteArrayOf()
            pdfUrlResponse.close()

            // Salva il pdf
            val uri = storageUtils.save(bytes, fileName, date.toString())

            // Controlla se ci sono variazioni
            var notified = false
            val pdfReader = PdfReader(bytes)
            val regex =
                Regex("""^${schoolClass}$""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
            for (pag in 0 until pdfReader.numberOfPages) {
                val content = PdfTextExtractor.getTextFromPage(pdfReader, pag + 1)
                if (regex.containsMatchIn(content)) {
                    if (uri != null) {
                        notificationUtils.sendPdfNotification(
                            uri,
                            "Variazioni",
                            "Ci sono variazioni."
                        )
                    } else {
                        notificationUtils.sendBrowserNotification(
                            pdfUrl,
                            "Variazioni",
                            "Ci sono variazioni. Errore salvataggio variazioni."
                        )
                    }
                    notified = true
                    break
                }
            }
            pdfReader.close()

            // Notifica nessuna variazione
            if (!notified && uri != null) {
                notificationUtils.sendPdfNotification(uri, "Variazioni", "Nessuna variazione.")
            } else if (!notified) {
                notificationUtils.sendBrowserNotification(
                    pdfUrl,
                    "Variazioni",
                    "Nessuna variazione. Errore salvataggio variazioni."
                )
            }

            return Result.success()
        } catch (th: Throwable) {
            notificationUtils.sendBrowserNotification(changesUrl, "Errore", "Errore non gestito")
            Log.e("CheckChanges", "Extraction error", th)
            return Result.failure()
        }
    }
}