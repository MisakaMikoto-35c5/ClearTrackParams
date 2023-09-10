package moe.wsl.lab.cleartrackparams

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import moe.wsl.lab.cleartrackparams.utils.SubscriptionManager
import moe.wsl.lab.cleartrackparams.utils.localdb.Subscription
import java.lang.Exception


class SubscriptionManagerActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private var dataSet: List<Subscription> = arrayListOf()
    private lateinit var subscriptionManager: SubscriptionManager
    private val viewAdapter = SubscriptionManagerRecyclerViewAdapter(this)
    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription_manager)
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptionManager.close()
    }

    private fun init() {
        Thread {
            initTools()
            initDataset()
            // initialized dataset then init components.
            runOnUiThread {
                initComponents()
            }
        }.start()
    }

    private fun initComponents() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.adapter = viewAdapter
        recyclerView.layoutManager = layoutManager
        recyclerView.hasPendingAdapterUpdates()

        findViewById<Button>(R.id.addButton).setOnClickListener {
            showEditSubscriptionDialog()
        }

        findViewById<Button>(R.id.updateAllButton).setOnClickListener {
            updateAllSubscriptions()
        }
    }

    private fun initTools() {
        subscriptionManager = SubscriptionManager(this)
    }

    private fun initDataset() {
        dataSet = subscriptionManager.getSubscriptionDb().subscriptionDao().getAll()
    }

    private fun onDataUpdate() {
        initDataset()
        runOnUiThread {
            recyclerView.hasPendingAdapterUpdates()
        }
    }

    private fun showEditSubscriptionDialog(data: Subscription? = null) {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_subscription, null)
        val editTextSubscriptionName = dialogView.findViewById<EditText>(R.id.editTextSubscriptionName)
        val editTextSubscriptionUrl = dialogView.findViewById<EditText>(R.id.editTextSubscriptionUrl)
        dialogBuilder.setView(dialogView)

        dialogBuilder.setPositiveButton(android.R.string.ok, null)
        dialogBuilder.setNegativeButton(android.R.string.cancel, null)
        if (data != null) {
            dialogBuilder.setNeutralButton("Delete",
                DialogInterface.OnClickListener { dialog, id ->
                    Thread {
                        subscriptionManager.getSubscriptionDb().subscriptionDao().delete(data)
                        onDataUpdate()
                    }.start()
                })
            editTextSubscriptionName.text.append(data.subscriptionName)
            editTextSubscriptionUrl.text.append(data.subscriptionURL)
            dialogBuilder.setTitle("Edit subscription")
        } else {
            dialogBuilder.setTitle("Add subscription")
        }
        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = editTextSubscriptionName.text.toString()
            val url = editTextSubscriptionUrl.text.toString()
            if (name.isEmpty() || url.isEmpty()) {
                Snackbar.make(
                    editTextSubscriptionName,
                    R.string.text_fields_can_not_be_empty,
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val progressDialog = ProgressDialog(this@SubscriptionManagerActivity)
            progressDialog.setTitle(getString(R.string.text_loading))
            progressDialog.setMessage(getString(R.string.text_loading))

            val worker = Thread {
                try {
                    if (data != null) {
                        subscriptionManager.updateSubscription(Subscription(
                            data.id,
                            name,
                            url,
                            0,
                            ""
                        ))
                    } else {
                        subscriptionManager.addSubscription(
                            name,
                            url
                        )
                    }
                    onDataUpdate()
                    runOnUiThread {
                        progressDialog.dismiss()
                        alertDialog.dismiss()
                    }
                } catch (e: Exception) {
                    Log.w("AddSubscription", "Error handled.")
                    e.printStackTrace()
                    runOnUiThread {
                        progressDialog.dismiss()
                        Snackbar.make(
                            editTextSubscriptionName,
                            "Error: $e",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

            progressDialog.setOnCancelListener {
                if (worker.isAlive) worker.stop()
            }
            progressDialog.show()
            Thread {
                Thread.sleep(1000)
                worker.start()
            }.start()
        }
    }

    private fun updateAllSubscriptions() {
        val progressDialog = ProgressDialog(this@SubscriptionManagerActivity)
        progressDialog.setTitle(getString(R.string.text_loading))
        progressDialog.setMessage(getString(R.string.text_loading))
        progressDialog.show()

        Thread {
            val errors = subscriptionManager.updateAllRules()
            Thread.sleep(1000)
            runOnUiThread {
                progressDialog.dismiss()
            }
        }.start()

    }

    class SubscriptionManagerRecyclerViewAdapter(private val parent: SubscriptionManagerActivity) :
        RecyclerView.Adapter<SubscriptionManagerRecyclerViewAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val itemTitle: TextView
            val itemDescription: TextView

            init {
                // Define click listener for the ViewHolder's View.
                itemTitle = view.findViewById(R.id.itemTitle)
                itemDescription = view.findViewById(R.id.itemDescription)
            }
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.subscription_item, viewGroup, false)

            return ViewHolder(view)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

            // Get element from your dataset at this position and replace the
            // contents of the view with that element
            val data = parent.dataSet[position]
            viewHolder.itemTitle.text = data.subscriptionName
            viewHolder.itemDescription.text = data.subscriptionURL
            viewHolder.itemView.setOnClickListener {
                parent.showEditSubscriptionDialog(data)
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount(): Int {
            return if (parent.dataSet == null) {
                0
            } else {
                parent.dataSet.size
            }
        }


    }
}