package pers.zhc.tools.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.regex_input_view.view.*
import pers.zhc.tools.R
import java.util.regex.PatternSyntaxException

/**
 * @author bczhc
 */
class RegexInputView : WrapLayout {
    private lateinit var shet: SmartHintEditText
    private lateinit var editText: EditText
    private lateinit var inputLayout: TextInputLayout
    private var cachedRegex: Regex? = null

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        val inflate = View.inflate(context, R.layout.regex_input_view, null).regex_et!!
        this.setView(inflate)
        shet = inflate

        editText = inflate.editText
        inputLayout = inflate.textInputLayout

        editText.doAfterTextChanged {
            val input = it!!.toString()
            cachedRegex = try {
                Regex(input).also {
                    // clear the error
                    inputLayout.error = null
                }
            } catch (e: PatternSyntaxException) {
                inputLayout.error = context.getString(R.string.regex_bad_pattern)
                return@doAfterTextChanged
            }
        }
    }

    var regex
        get() = if (inputLayout.error == null /* not wrong pattern */) {
            this.cachedRegex
        } else {
            null
        }
        set(regex) {
            regex?.run {
                editText.setText(regex.pattern)
            }
        }
}