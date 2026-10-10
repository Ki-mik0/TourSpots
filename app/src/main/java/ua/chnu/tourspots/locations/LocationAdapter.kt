package ua.chnu.tourspots.locations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ua.chnu.tourspots.R
import ua.chnu.tourspots.data.Location
import ua.chnu.tourspots.databinding.ItemLocationBinding
import java.util.Locale

/** Feed of locations ("стрічка локацій"). */
class LocationAdapter(private val onClick: (Location) -> Unit) :
    ListAdapter<Location, LocationAdapter.Holder>(Diff) {

    class Holder(val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val location = getItem(position)
        val context = holder.itemView.context
        with(holder.binding) {
            ivCategory.setImageResource(Categories.background(context, location.categoryId))
            tvTitle.text = location.title
            tvMeta.text = context.getString(
                R.string.location_meta,
                Categories.name(context, location.categoryId),
                location.authorName ?: context.getString(R.string.author_system),
            )
            tvDescription.text = location.description
            tvRating.text = if (location.reviewCount == 0) "☆ —"
            else String.format(Locale.getDefault(), "★ %.1f", location.averageRating)
            root.setOnClickListener { onClick(location) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(oldItem: Location, newItem: Location) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Location, newItem: Location) = oldItem == newItem
    }
}
