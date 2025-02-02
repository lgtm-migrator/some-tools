package pers.zhc.tools.app

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.tools_activity_main.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.charucd.CharLookupActivity
import pers.zhc.tools.clipboard.Clip
import pers.zhc.tools.pi.Pi
import pers.zhc.tools.test.RegExpTest
import pers.zhc.tools.test.SysInfo
import pers.zhc.tools.test.UnicodeTable
import pers.zhc.tools.test.toast.ToastTest
import pers.zhc.tools.test.typetest.TypeTest
import pers.zhc.tools.tools.CharsSplitter

/**
 * @author bczhc
 */
class SmallToolsListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tools_activity_main)

        val recyclerView = recycler_view!!
        recyclerView.layoutManager = LinearLayoutManager(this)

        val activities = listOf(
            ActivityItem(R.string.generate_pi, Pi::class.java),
            ActivityItem(R.string.toast, ToastTest::class.java),
            ActivityItem(R.string.put_in_clipboard, Clip::class.java),
            ActivityItem(R.string.type_test, TypeTest::class.java),
            ActivityItem(R.string.regular_expression_test, RegExpTest::class.java),
            ActivityItem(R.string.sys_info_label, SysInfo::class.java),
            ActivityItem(R.string.unicode_table_label, UnicodeTable::class.java),
            ActivityItem(R.string.chars_splitter_label, CharsSplitter::class.java),
            ActivityItem(R.string.char_ucd_lookup_activity_label, CharLookupActivity::class.java),
        )

        recyclerView.adapter = AppMenuAdapter(this, activities)
    }
}