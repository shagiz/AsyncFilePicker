package org.shagi.filepicker

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import org.shagi.filepicker.filepicker.CustomActionItem
import org.shagi.filepicker.filepicker.FilePickerDialog
import org.shagi.filepicker.filepicker.FilePickerFragment
import org.shagi.filepicker.filepicker.RxFilePicker
import timber.log.Timber
import java.io.File

class MainActivity : AppCompatActivity(), RxFilePicker.OnLoadingListener {

    private var keys: MutableList<Long> = ArrayList<Long>()

    private var lastMillis = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("DebugTag, onCreate $this")

        setContentView(R.layout.activity_main)
        val listener: View.OnClickListener

        if (savedInstanceState == null) {
            val filePickerDialog = FilePickerDialog.newInstance().apply {
                addCustomAction(
                        CustomActionItem(R.drawable.ic_folder,
                                R.string.fpd_load_from_medical_note_documents,
                                View.OnClickListener {
                                    Toast.makeText(context, "1 $context", Toast.LENGTH_SHORT).show()
                                })
                )
                addCustomAction(
                        CustomActionItem(R.drawable.ic_folder,
                                R.string.fpd_load_from_medical_note_documents,
                                View.OnClickListener {
                                    Toast.makeText(context, "2  $context", Toast.LENGTH_SHORT).show()
                                })
                )
            }
            listener = View.OnClickListener {
                val pickerFragment = FilePickerFragment.getFragment(supportFragmentManager)

                pickerFragment.setOnLoadingListener(this)
                pickerFragment.use(filePickerDialog)
                pickerFragment.show()
            }
        } else {
            keys = savedInstanceState.getLongArray(ARG_KEYS).toMutableList()
            val fragment = FilePickerFragment.getFragment(supportFragmentManager)
            fragment.setOnLoadingListener(this)
            listener = View.OnClickListener { fragment.show() }
        }

        image_1.setOnClickListener(listener)
        image_2.setOnClickListener(listener)
        image_3.setOnClickListener(listener)
        image_4.setOnClickListener(listener)

        Timber.d("listener ${image_1.hasOnClickListeners()}")
    }

    override fun onLoadingStart(key: Long) {
        progress.visibility = View.VISIBLE
        keys.add(key)
        Timber.d("DebugTag, main onLoadingStart $this with key $key")
    }

    override fun onLoadingSuccess(key: Long, file: File) {
        progress.visibility = View.GONE
        val imageView: ImageView = when (keys.size) {
            0 -> image_1
            1 -> image_2
            2 -> image_3
            3 -> image_4
            else -> image_1
        }

        keys.remove(key)

        Picasso.with(this)
                .load(file)
                .into(imageView)

        Timber.d("DebugTag, main onLoadingSuccess $this with key $key")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray(ARG_KEYS, keys.toLongArray())
    }

    override fun onLoadingFailure(key: Long) {
        progress.visibility = View.GONE
        Timber.d("DebugTag, main onLoadingFailure $this with key $key")
    }

    companion object {
        const val ARG_KEYS = "arg_keys"
    }
}
