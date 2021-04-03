package pers.zhc.tools.diary

    import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.diary_attachment_adding_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils
import java.util.*

class DiaryAttachmentAddingActivity : DiaryBaseActivity() {
    private lateinit var descriptionET: EditText
    private lateinit var titleET: EditText
    private lateinit var fileIdentifierList: LinkedList<String>
    private lateinit var fileListLL: LinearLayout
    lateinit var linearLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.diary_attachment_adding_activity)
        titleET = title_et!!
        fileListLL = file_list_ll!!
        descriptionET = description_et!!
        val createAttachmentBtn = create_attachment_btn!!
        val pickFileBtn = pick_file_btn

        fileIdentifierList = LinkedList<String>()

        pickFileBtn.setOnClickListener {
            val intent = Intent(this, FileLibraryActivity::class.java)
            intent.putExtra("pick", true)
            // pick file from the file library
            startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
        }

        createAttachmentBtn.setOnClickListener {
            createAttachment()
            ToastUtils.show(this, R.string.creating_succeeded)
            finish()
        }
    }

    private fun createAttachment() {
        val attachmentId = System.currentTimeMillis()
        diaryDatabase.beginTransaction()
        val statement =
            diaryDatabase.compileStatement("INSERT INTO diary_attachment_file_reference(attachment_id, file_identifier)\nVALUES (?, ?)")
        fileIdentifierList.forEach {
            statement.reset()
            statement.bind(1, attachmentId)
            statement.bindText(2, it)
            statement.step()
        }
        diaryDatabase.commit()
        statement.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data!!
        when (requestCode) {
            RequestCode.START_ACTIVITY_0 -> {
                // pick file from the file library
                val fileInfo = data.getSerializableExtra("fileInfo") as FileLibraryActivity.FileInfo
                val filePreviewView = FileLibraryActivity.getFilePreviewView(this, fileInfo)
                filePreviewView.background = getDrawable(R.drawable.view_stroke)
                fileListLL.addView(filePreviewView)
                fileIdentifierList.add(fileInfo.identifier)
            }
            else -> {

            }
        }
    }
}