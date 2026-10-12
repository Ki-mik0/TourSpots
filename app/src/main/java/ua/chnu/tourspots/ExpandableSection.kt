package ua.chnu.tourspots

import android.view.View

/**
 * A card section whose body collapses behind its header row. The chevron turns to show the state.
 * Collapsing only hides views: the content (for example the stopwatch) keeps running.
 */
class ExpandableSection(
    header: View,
    private val body: View,
    private val chevron: View,
    initiallyExpanded: Boolean,
) {

    var isExpanded: Boolean = initiallyExpanded
        private set

    init {
        header.setOnClickListener { toggle() }
        render()
    }

    private fun toggle() {
        isExpanded = !isExpanded
        render()
    }

    private fun render() {
        body.visibility = if (isExpanded) View.VISIBLE else View.GONE
        chevron.rotation = if (isExpanded) 180f else 0f
    }
}
