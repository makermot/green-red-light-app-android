package com.epfl.esl.a1_c_green_red_light

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemAdapterFriends(val context: Context, val items: ArrayList<String>,
                         val items_2: ArrayList<Bitmap>) :
    RecyclerView.Adapter<ItemAdapterFriends.ViewHolder>()
{
    /**
     * Inflates the item views which is designed in xml layout file
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.friend_custom_row,
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
        println("is in OnBindViewHolder")
        val item_position = items.get(position)
        val item_2_position = items_2.get(position)
        holder.tvItem.text = item_position
        holder.tvItem_2.setImageBitmap(item_2_position)
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
        var tvItem = view.findViewById<TextView>(R.id.friend)!!
        var tvItem_2 = view.findViewById<ImageView>(R.id.friendImage)!!
    }
}