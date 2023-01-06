/*package com.epfl.esl.a1_c_green_red_light

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemAdapterFriends(val context: Context, val items: ArrayList<String>,
                  val items_2: ArrayList<String>) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>()
{
    /**
     * Inflates the item views which is designed in xml layout file
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.stat_custom_row,
                parent,
                false
            )
        )
    }
    /**
     * Binds each item in the ArrayList to a view
     *
     * This new ViewHolder should be constructed with a new View that can
    represent the items
     * of the given type. You can either create a new View manually or inflate it
    from an XML
     * layout file.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item_position = items.get(position)
        val item_2_position = items_2.get(position)
        val item_3_position = items_3.get(position)
        val item_4_position = items_4.get(position)
        val item_5_position = items_5.get(position)
        val item_6_position = items_6.get(position)
        holder.tvItem.text = item_position
        holder.tvItem_2.text = item_2_position
        holder.tvItem_3.text = item_3_position
        holder.tvItem_4.text = item_4_position
        holder.tvItem_5.text = item_5_position
        holder.tvItem_6.text = item_6_position
    }
    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return items.size}
    /**
     * A ViewHolder describes an item view and metadata about
    its place within the RecyclerView.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each item to
        var tvItem = view.findViewById<TextView>(R.id.title_stat)
        var tvItem_2 = view.findViewById<TextView>(R.id.coordinates_start)
        var tvItem_3 = view.findViewById<TextView>(R.id.coordinates_finish)
        var tvItem_4 = view.findViewById<TextView>(R.id.winner)
        var tvItem_5 = view.findViewById<TextView>(R.id.elapsed_time)
        var tvItem_6 = view.findViewById<TextView>(R.id.players)
    }
}*/