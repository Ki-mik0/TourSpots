package ua.chnu.tourspots

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Lab 2: switching the app language between the strings.xml variants (values/, values-en/). */
object LocaleHelper {

    data class Language(val tag: String, val label: String, @DrawableRes val flag: Int)

    val languages = listOf(
        Language("uk", "UA", R.drawable.flag_uk),
        Language("en", "EN", R.drawable.flag_en),
    )

    /** Index of the language currently in use (default: Ukrainian, the unqualified locale). */
    fun currentIndex(): Int {
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.language ?: return 0
        return languages.indexOfFirst { it.tag == tag }.coerceAtLeast(0)
    }

    /** Persists the choice (autoStoreLocales) and recreates activities with the new resources. */
    fun apply(language: Language) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    /** Sets up the language Spinner placed as an action view in the top right corner of the toolbar. */
    fun bindMenuSpinner(context: Context, menu: Menu) {
        val spinner = menu.findItem(R.id.action_language).actionView as Spinner
        spinner.adapter = Adapter(context)
        spinner.setSelection(currentIndex(), false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != currentIndex()) apply(languages[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    /** Spinner adapter showing a flag next to the language code. */
    class Adapter(context: Context) : ArrayAdapter<Language>(context, 0, languages) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup) =
            bind(position, convertView, parent, textColor = 0xFFFFFFFF.toInt())

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
            bind(position, convertView, parent, textColor = 0xFF000000.toInt())

        private fun bind(position: Int, convertView: View?, parent: ViewGroup, textColor: Int): View {
            val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_language, parent, false)
            val language = getItem(position)!!
            view.findViewById<ImageView>(R.id.ivFlag).setImageResource(language.flag)
            view.findViewById<TextView>(R.id.tvLanguage).apply {
                text = language.label
                setTextColor(textColor)
            }
            return view
        }
    }
}
