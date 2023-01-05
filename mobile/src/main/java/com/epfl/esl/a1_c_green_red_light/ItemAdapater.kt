/*class ItemAdapter(val context: Context, val items: ArrayList<String>,
                  val items_2: ArrayList<String>, val items_3: ArrayList<String>) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
    /**
     * Inflates the item views which is designed in xml layout file
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_custom_row,
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
        holder.tvItem.text = item_position
        holder.tvItem_2.text = item_2_position
        holder.tvItem_3.text = item_3_position
    }
    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return items.size
    }
        /**
         * A ViewHolder describes an item view and metadata about
        its place within the RecyclerView.
         */
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            // Holds the TextView that will add each item to
            var tvItem = view.findViewById<TextView>(R.id.activity_status)
            var tvItem_2 = view.findViewById<TextView>(R.id.exerciseDateTime_firebase)
            var tvItem_3 = view.findViewById<TextView>(R.id.smartwatchOrBelt)
        }
    }*/