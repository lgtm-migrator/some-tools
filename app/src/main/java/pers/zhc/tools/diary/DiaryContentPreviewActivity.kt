package pers.zhc.tools.diary

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import kotlinx.android.synthetic.main.diary_content_preview_activity.*
import pers.zhc.tools.R

/**
 * @author bczhc
 */
class DiaryContentPreviewActivity : DiaryBaseActivity() {
    private lateinit var contentTV: TextView
    private var dateInt: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary_content_preview_activity)

        contentTV = content_tv!!

        val intent = intent
        val dateInt = intent.getIntExtra("dateInt", -1)
        if (dateInt == -1) throw RuntimeException("No dateInt provided.")
        this.dateInt = dateInt

        val content = fetchContent(dateInt)
        contentTV.text = content
    }

    private fun fetchContent(dateInt: Int): String {
        val statement = this.diaryDatabase.compileStatement("SELECT content\nFROM diary\nWHERE \"date\" IS ?")
        statement.bind(1, dateInt)
        val cursor = statement.cursor
        cursor.step()
        val content = cursor.getText(statement.getIndexByColumnName("content"))
        statement.release()
        return content
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.diary_content_preview_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> {
                val intent = Intent(this, DiaryTakingActivity::class.java)
                intent.putExtra("dateInt", dateInt)
                startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data!!
        when (requestCode) {
            RequestCode.START_ACTIVITY_0 -> {
                // start diary taking activity
                // refresh the current content text view
                val content = fetchContent(dateInt)
                contentTV.text = content
            }
            else -> {
            }
        }
    }
}