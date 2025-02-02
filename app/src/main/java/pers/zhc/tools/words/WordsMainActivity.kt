package pers.zhc.tools.words

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerView
import kotlinx.android.synthetic.main.words_activity.*
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.jni.JNI.Utf8
import pers.zhc.tools.utils.*
import java.io.File

/**
 * @author bczhc
 */
class WordsMainActivity : BaseActivity() {
    private val itemList = ArrayList<Item>()
    private lateinit var listAdapter: MyAdapter

    private val launchers = object {
        val importFilePicker = initImportFilePickerLauncher()
        val exportFilePicker = initExportFilePickerLauncher()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (notificationId == null) {
            notificationId = System.currentTimeMillis().hashCode()
        }

        setContentView(R.layout.words_activity)

        checkAndInitDatabase()
        showNotification()

        val recyclerView = recycler_view!!
        val fastScroller = fast_scroller!!
        val fastScrollerThumb = fast_scroller_thumb!!

        updateItems()
        listAdapter = MyAdapter(this, itemList)

        recyclerView.adapter = listAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        listAdapter.setOnItemLongClickListener { position, view ->

            val popupMenu = PopupMenuUtil.createPopupMenu(this, view, R.menu.word_item_popup_menu)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete -> {
                        DialogUtil.createConfirmationAlertDialog(this, { _, _ ->

                            deleteRecord(itemList[position].word)
                            itemList.removeAt(position)
                            listAdapter.notifyItemRemoved(position)

                        }, R.string.words_delete_confirmation_dialog).show()
                    }
                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }

        fastScroller.setupWithRecyclerView(recyclerView, { position ->
            val chars = CodepointIterator(itemList[position].word)
            val text = if (chars.hasNext()) {
                Utf8.codepoint2str(chars.next()).uppercase()
            } else {
                ""
            }
            FastScrollItemIndicator.Text(text)
        })
        fastScroller.useDefaultScroller = false
        fastScroller.itemIndicatorSelectedCallbacks += object : FastScrollerView.ItemIndicatorSelectedCallback {
            override fun onItemIndicatorSelected(
                indicator: FastScrollItemIndicator,
                indicatorCenterY: Int,
                itemPosition: Int
            ) {
               recyclerView.stopScroll()
               recyclerView.scrollToPosition(itemPosition)
            }
        }
        fastScrollerThumb.setupWithFastScroller(fastScroller)
    }

    class Item(
        val word: String
    )

    private fun deleteRecord(word: String) {
        database!!.execBind("DELETE FROM word WHERE word IS ?", arrayOf(word))
    }

    private fun updateItems() {
        itemList.clear()

        val statement = database!!.compileStatement("SELECT word FROM word")
        val cursor = statement.cursor
        while (cursor.step()) {
            val word = cursor.getText(0)
            itemList.add(Item(word))
        }
        statement.release()

        itemList.sortBy {
            it.word.uppercase()
        }
    }

    private fun showNotification() {
        val intent = Intent(this, DialogShowActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, RequestCode.START_ACTIVITY_0, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        )

        val notification = NotificationCompat.Builder(this, MyApplication.NOTIFICATION_CHANNEL_ID_UNIVERSAL).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(getString(R.string.words_label))
            setContentIntent(pi)
            setOngoing(true)
        }.build()

        val nm = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId!!, notification)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.words_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.import_ -> {
                importAction()
            }
            R.id.export -> {
                exportAction()
            }
            R.id.create_shortcut -> {
                createShortcutAction()
            }
            else -> {
            }
        }
        return true
    }

    private fun createShortcutAction() {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            ToastUtils.show(this, R.string.words_not_support_pinned_shortcut_toast)
            return
        }

        val intent = Intent("NONE_ACTION")
        intent.setClass(this, DialogShowActivity::class.java)
        val shortcut = ShortcutInfoCompat.Builder(this, "shortcut.words").apply {
            setIcon(IconCompat.createWithResource(this@WordsMainActivity, R.drawable.ic_launcher_foreground))
            setIntent(intent)
            setShortLabel(getString(R.string.words_add_word_shortcut_label))
        }.build()
        ShortcutManagerCompat.requestPinShortcut(this, shortcut, null)
    }

    private fun importAction() {
        launchers.importFilePicker.launch(FilePicker.PICK_FILE)
    }

    private fun exportAction() {
        launchers.exportFilePicker.launch(FilePicker.PICK_FOLDER)
    }

    private fun initImportFilePickerLauncher(): ActivityResultLauncher<Int> {
        return FilePicker.getLauncher(this) {
            it ?: return@getLauncher

            Thread {
                FileUtil.copy(it, databasePath)
                updateItems()
                runOnUiThread {
                    listAdapter.notifyDataSetChanged()
                    ToastUtils.show(this, R.string.importing_succeeded)
                }
            }.start()
        }
    }

    private fun initExportFilePickerLauncher(): ActivityResultLauncher<Int> {
        return FilePicker.getLauncherWithFilename(this) { path, filename ->
            path ?: return@getLauncherWithFilename

            val outputFile = File(path, filename)
            Thread {
                FileUtil.copy(File(databasePath), outputFile)
                ToastUtils.show(this, R.string.exporting_succeeded)
            }.start()
        }
    }

    class MyAdapter(private val ctx: Context, private val itemList: List<Item>) :
        AdapterWithClickListener<MyAdapter.MyViewHolder>() {

        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            lateinit var tv: TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val inflate = LayoutInflater.from(ctx).inflate(android.R.layout.simple_list_item_1, parent, false).apply {
                this.setBaseLayoutSizeMW()
                this.setBackgroundResource(R.drawable.selectable_bg)
            }
            val viewHolder = MyViewHolder(inflate)
            viewHolder.tv = inflate.findViewById(android.R.id.text1)!!
            return viewHolder
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.tv.text = itemList[position].word
        }

        override fun getItemCount(): Int {
            return itemList.size
        }
    }

    companion object {

        private var notificationId: Int? = null

        var database: SQLite3? = null
        private lateinit var databasePath: String

        fun init(ctx: Context) {
            databasePath = Common.getInternalDatabaseDir(ctx, "words.db").path
        }

        fun checkAndInitDatabase() {
            if (database == null) {
                // init
                database = SQLite3.open(databasePath)
                initDatabase(database!!)
            }
        }

        private fun initDatabase(database: SQLite3) {
            database.exec(
                """CREATE TABLE IF NOT EXISTS word
(
    word          TEXT NOT NULL PRIMARY KEY,
    addition_time INTEGER
)"""
            )
        }
    }
}