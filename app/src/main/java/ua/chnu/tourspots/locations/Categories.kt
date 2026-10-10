package ua.chnu.tourspots.locations

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.widget.Toast
import ua.chnu.tourspots.R
import java.util.Locale

/** Category ids in the DB are 1-based positions of the localized @array/categories. */
object Categories {

    fun names(context: Context): Array<String> = context.resources.getStringArray(R.array.categories)

    fun name(context: Context, categoryId: Int): String = names(context).getOrElse(categoryId - 1) { "?" }

    fun idForPosition(position: Int): Int = position + 1

    fun positionForId(categoryId: Int): Int = categoryId - 1

    fun background(context: Context, categoryId: Int): Int {
        val array = context.resources.obtainTypedArray(R.array.category_backgrounds)
        try {
            return array.getResourceId(positionForId(categoryId), R.drawable.bg_main)
        } finally {
            array.recycle()
        }
    }
}

/** "Перегляд на карті": opens any installed maps app at the given point (implicit geo: intent). */
object MapLauncher {

    fun formatCoordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude)

    fun show(context: Context, latitude: Double, longitude: Double, label: String) {
        val point = String.format(Locale.ROOT, "%f,%f", latitude, longitude)
        val uri = "geo:$point?q=$point(${Uri.encode(label)})".toUri()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // No maps app: fall back to the browser version of OpenStreetMap.
            val web = "https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=14/$latitude/$longitude".toUri()
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, web))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_map_app, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
