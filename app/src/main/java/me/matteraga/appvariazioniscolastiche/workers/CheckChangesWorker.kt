package me.matteraga.appvariazioniscolastiche.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.itextpdf.text.BaseColor
import com.itextpdf.text.pdf.PdfGState
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import me.matteraga.appvariazioniscolastiche.R
import me.matteraga.appvariazioniscolastiche.utilities.NotificationUtils
import me.matteraga.appvariazioniscolastiche.utilities.StorageUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.io.IOException
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
                    date,
                    "Variazioni",
                    "PDF variazioni non trovato per il ${date}.",
                    R.drawable.ic_unpublished
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

            // Controlla se ci sono variazioni
            var changes = false
            val reader = PdfReader(bytes)
            val stream = ByteArrayOutputStream()
            val stamper = PdfStamper(reader, stream)
            val regex = Regex(
                """^[1-5][A-Za-z]{1,3}$""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
            )
            for (pag in 1 until reader.numberOfPages + 1) {
                val content = PdfTextExtractor.getTextFromPage(reader, pag)
                val schoolClasses = regex.findAll(content).map {
                    it.value
                }.toList()

                val canvas = stamper.getOverContent(pag)
                for (schoolClass in schoolClasses.indices) {
                    if (schoolClasses[schoolClass].equals(this.schoolClass, true)) {
                        changes = true
                        // Evidenzia la riga
                        canvas.apply {
                            saveState()
                            setColorFill(BaseColor.YELLOW)
                            setGState(PdfGState().apply { setFillOpacity(0.3f) })
                            rectangle(72.5, 718.0 - (13.75 * schoolClass), 449.0, 13.0)
                            fill()
                            restoreState()
                        }
                    }
                }
            }
            stamper.close()
            reader.close()

            // Salva il pdf con eventuali rettangoli gialli
            val uri = storageUtils.save(stream.toByteArray(), fileName, date.toString())

            val openPDF = inputData.getBoolean("openPDF", false)
            // Apri il pdf
            if (openPDF && uri != null) {
                val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(pdfIntent)
                return Result.success()
            }

            // Invia notifica
            if (changes) {
                if (uri != null) {
                    notificationUtils.sendPdfNotification(
                        uri,
                        date,
                        "Variazioni",
                        "Ci sono variazioni per il ${date}.",
                        R.drawable.ic_check_circle
                    )
                } else {
                    notificationUtils.sendBrowserNotification(
                        pdfUrl,
                        date,
                        "Variazioni",
                        "Ci sono variazioni per il ${date}. Errore salvataggio variazioni.",
                        R.drawable.ic_warning
                    )
                }
            } else {
                if (uri != null) {
                    notificationUtils.sendPdfNotification(
                        uri,
                        date,
                        "Variazioni",
                        "Nessuna variazione per il ${date}.",
                        R.drawable.ic_unpublished
                    )
                } else {
                    notificationUtils.sendBrowserNotification(
                        pdfUrl,
                        date,
                        "Variazioni",
                        "Nessuna variazione per il ${date}. Errore salvataggio variazioni.",
                        R.drawable.ic_warning
                    )
                }
            }

            return Result.success()
        } catch (ex: IOException) {
            notificationUtils.sendOpenAppNotification(
                date,
                "Errore",
                "Errore di connessione per il ${date}.",
                R.drawable.ic_cancel
            )
            Log.e("CheckChanges", "Connection error", ex)
            return Result.failure()
        } catch (th: Throwable) {
            notificationUtils.sendBrowserNotification(
                changesUrl,
                date,
                "Errore",
                "Errore non gestito per il ${date}.",
                R.drawable.ic_cancel
            )
            Log.e("CheckChanges", "Extraction error", th)
            return Result.failure()
        }
    }
}