package example.jllarraz.com.passportreader.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import example.jllarraz.com.passportreader.R

class InfoAlert(context: Context) : MessageBox(context, Icon.Info)
class ErrorAlert(context: Context) : MessageBox(context, Icon.Error)

open class MessageBox(context: Context, icon: Icon = Icon.None) : Dialog(context) {

    enum class Icon {
        None, Info, Error
    }

    var icon: Icon = icon
        set(icon) {
            ivBanner.visibility = View.VISIBLE
            when (icon) {
                Icon.Info -> {
                    ivBanner.setImageResource(R.drawable.ic_info)
                    ivBanner.setBackgroundColor(Color.parseColor("#2196F3"))
                }
                Icon.Error -> {
                    ivBanner.setImageResource(R.drawable.ic_cross)
                    ivBanner.setBackgroundColor(Color.parseColor("#DA5F6A"))
                }
                else -> ivBanner.visibility = View.GONE
            }
            field = icon
        }

    val title: CharSequence
        get() : CharSequence {
            return tvTitle.text
        }

    var message: CharSequence
        get() : CharSequence {
            return tvMsg.text
        }
        set(msg) {
            tvMsg.text = msg
            tvMsg.show()
            tvTitle.setTopMargin(10)
            tvTitle.setBottomMargin(0)
        }


    private var tvTitle: TextView
    private var tvMsg: TextView
    private var ivBanner: ImageView
    private var btnNegative: Button
    private var btnPositive: Button

    init {
        this.setCancelable(false)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.layout_message_box)
        window?.setLayout(MATCH_PARENT, WRAP_CONTENT)

        tvTitle     = this.findViewById(R.id.lmb_title)
        tvMsg       = this.findViewById(R.id.lmb_msg)
        ivBanner    = this.findViewById(R.id.lmb_banner)
        btnNegative = this.findViewById(R.id.lmb_btn_negative)
        btnPositive = this.findViewById(R.id.lmb_btn_positive)
        btnPositive.hide()
        btnNegative.hide()

        tvMsg.text = ""
        this.setTitle("")
        this.icon = icon
    }

    override fun setTitle(title: CharSequence?) {
        tvTitle.text = title
        tvTitle.show()
        if(title == null || title.isEmpty()) {
            tvTitle.hide()
        }
    }

    fun setPositiveButton(text:CharSequence, listener: (dialog: MessageBox, view: View) -> Unit) {
        btnPositive.text = text
        btnPositive.show()
        btnPositive.setOnClickListener{
            listener(this, it)
        }

        if(!btnNegative.isVisible()) {
            btnPositive.alignCenter()
        }
        else{
            btnNegative.alignLeft()
        }
    }

    fun setNegativeButton(text:CharSequence, listener: (dialog: MessageBox, view: View) -> Unit){
        btnNegative.text = text
        btnNegative.show()
        btnNegative.setOnClickListener {
            listener(this, it)
        }

        if(!btnPositive.isVisible()) {
            btnNegative.alignCenter()
        }
        else{
            btnPositive.alignRight()
        }
    }

    override fun show() {
        if(!btnPositive.isVisible() && !btnNegative.isVisible()) {
            setPositiveButton("OK")  { dialog, _ ->
                dialog.dismiss()
            }
        }
        if(tvMsg.text.isEmpty()) {
            tvTitle.setTopMargin(40)
            tvTitle.setBottomMargin(40)
            tvMsg.hide()
        }
        super.show()
    }

    companion object {
        private fun dp(c: Context, value: Int) : Int {
            return (c.resources.displayMetrics.density * value).toInt()
        }

        private fun TextView.setTopMargin(margin: Int) {
            val params = layoutParams as RelativeLayout.LayoutParams
            params.topMargin = dp(context, margin)
            layoutParams = params
        }

        private fun TextView.show() {
            visibility = View.VISIBLE
        }

        private fun TextView.hide() {
            visibility = View.GONE
        }

        private fun TextView.setBottomMargin(margin: Int) {
            val params = layoutParams as RelativeLayout.LayoutParams
            params.bottomMargin = dp(context, margin)
            layoutParams = params
        }

        private fun Button.isVisible() : Boolean {
            return visibility == View.VISIBLE
        }

        private fun Button.show() {
            visibility = View.VISIBLE
        }

        private fun Button.hide() {
            visibility = View.GONE
        }

        private fun Button.alignCenter() {
            val params = layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
            params.removeRule(RelativeLayout.ALIGN_PARENT_END)
            params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
            layoutParams = params
        }

        private fun Button.alignRight() {
            val params = layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
            params.removeRule(RelativeLayout.CENTER_HORIZONTAL)
            layoutParams = params
        }

        private fun Button.alignLeft() {
            val params = layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
            params.removeRule(RelativeLayout.CENTER_HORIZONTAL)
            layoutParams = params
        }
    }
}