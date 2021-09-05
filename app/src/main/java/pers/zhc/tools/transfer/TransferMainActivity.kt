package pers.zhc.tools.transfer

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.transfer_main_activity.*
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI

/**
 * @author bczhc
 */
class TransferMainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transfer_main_activity)

        val sendButton = send_btn!!
        val receiveButton = receive_btn!!
        val ipInfoTV = ip_info_tv!!

        ipInfoTV.text = JNI.Transfer.getLocalIpInfo()!!

        sendButton.setOnClickListener {
            startActivity(Intent(this, TransferSendActivity::class.java))
        }
        receiveButton.setOnClickListener {
            startActivity(Intent(this, TransferReceiveActivity::class.java))
        }
    }
}